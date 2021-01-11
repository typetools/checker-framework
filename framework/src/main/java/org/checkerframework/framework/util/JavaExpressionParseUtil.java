package org.checkerframework.framework.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ArrayCreation;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;
import org.plumelib.util.CollectionsPlume;

/**
 * Helper methods to parse a string that represents a restricted Java expression.
 *
 * @checker_framework.manual #java-expressions-as-arguments Writing Java expressions as annotation
 *     arguments
 * @checker_framework.manual #dependent-types Annotations whose argument is a Java expression
 *     (dependent type annotations)
 */
public class JavaExpressionParseUtil {

    /** Regular expression for a formal parameter use. */
    protected static final String PARAMETER_REGEX = "#([1-9][0-9]*)";

    /** Anchored pattern for a formal parameter use. */
    protected static final Pattern ANCHORED_PARAMETER_PATTERN =
            Pattern.compile("^" + PARAMETER_REGEX + "$");

    /** Unanchored; can be used to find all formal parameter uses. */
    protected static final Pattern UNANCHORED_PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEX);

    /**
     * Parsable replacement for parameter references. It is parseable because it is a Java
     * identifier.
     */
    private static final String PARAMETER_REPLACEMENT = "_param_";

    /** The length of {@link #PARAMETER_REPLACEMENT}. */
    private static final int PARAMETER_REPLACEMENT_LENGTH = PARAMETER_REPLACEMENT.length();

    /**
     * Parse a string and return its representation as a {@link JavaExpression}, or throw a {@link
     * JavaExpressionParseException}.
     *
     * @param expression a Java expression to parse
     * @param context information about any receiver and arguments
     * @param localScope path to local scope to use
     * @param useLocalScope whether {@code localScope} should be used to resolve identifiers
     */
    public static JavaExpression parse(
            String expression,
            JavaExpressionContext context,
            TreePath localScope,
            boolean useLocalScope)
            throws JavaExpressionParseException {

        Expression expr;
        try {
            expr = StaticJavaParser.parseExpression(replaceParameterSyntax(expression));
        } catch (ParseProblemException e) {
            throw constructParserException(expression, "is an invalid expression");
        }

        JavaExpression result;
        try {
            context = context.copyAndSetUseLocalScope(useLocalScope);
            ProcessingEnvironment env = context.checkerContext.getProcessingEnvironment();
            result = expr.accept(new ExpressionToJavaExpressionVisitor(localScope, env), context);
        } catch (ParseRuntimeException e) {
            // Convert unchecked to checked exception. Visitor methods can't throw checked
            // exceptions. They override the methods in the superclass, and a checked exception
            // would change the method signature.
            throw e.getCheckedException();
        }
        if (result instanceof ClassName
                && !expression.endsWith(".class")
                // At a call site, "#1" may be transformed to "Something.class", so don't throw an
                // exception in that case.
                && !ANCHORED_PARAMETER_PATTERN.matcher(expression).matches()) {
            throw constructParserException(
                    expression,
                    String.format(
                            "a class name cannot terminate a Java expression string, where result=%s [%s]",
                            result, result.getClass()));
        }
        return result;
    }

    /**
     * Replaces every occurrence of "#NUMBER" with "_param_NUMBER" where NUMBER is the 1-based index
     * of a formal parameter.
     *
     * <p>Note that this does replacement even within strings.
     *
     * @param expression a Java expression in which to replace
     * @return the Java expression, with formal parameter references like "#2" replaced by an
     *     identifier like "_param_2"
     */
    private static String replaceParameterSyntax(String expression) {
        String updatedExpression = expression;

        for (Integer integer : parameterIndices(expression)) {
            updatedExpression =
                    updatedExpression.replaceAll("#" + integer, PARAMETER_REPLACEMENT + integer);
        }

        return updatedExpression;
    }

    /**
     * A visitor class that converts a JavaParser {@link Expression} to a {@link JavaExpression}.
     */
    private static class ExpressionToJavaExpressionVisitor
            extends GenericVisitorWithDefaults<JavaExpression, JavaExpressionContext> {

        /**
         * The Java program element that is annotated by an annotation that contains the expression
         * that is being translated.
         */
        private final TreePath annotatedConstruct;
        /** The processing environment. */
        private final ProcessingEnvironment env;
        /** The resolver. Computed from the environment, but lazily initialized. */
        private @MonotonicNonNull Resolver resolver = null;
        /** The type utilities. */
        private final Types types;

        /**
         * Create a new ExpressionToJavaExpressionVisitor.
         *
         * @param annotatedConstruct path to the expression
         * @param env the processing environment
         */
        ExpressionToJavaExpressionVisitor(TreePath annotatedConstruct, ProcessingEnvironment env) {
            this.annotatedConstruct = annotatedConstruct;
            this.env = env;
            this.types = env.getTypeUtils();
        }

        /** Sets the {@code resolver} field if necessary. */
        private void setResolverField() {
            if (resolver == null) {
                resolver = new Resolver(env);
            }
        }

        /** If the expression is not supported, throw a {@link ParseRuntimeException} by default. */
        @Override
        public JavaExpression defaultAction(
                com.github.javaparser.ast.Node n, JavaExpressionContext context) {
            String message = "is not a supported expression";
            if (context.parsingMember) {
                message += " in a context with parsingMember=true";
            }
            throw new ParseRuntimeException(constructParserException(n.toString(), message));
        }

        @Override
        public JavaExpression visit(NullLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getNullType(), (Object) null);
        }

        @Override
        public JavaExpression visit(IntegerLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.INT), expr.asNumber());
        }

        @Override
        public JavaExpression visit(LongLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.LONG), expr.asNumber());
        }

        @Override
        public JavaExpression visit(CharLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.CHAR), expr.asChar());
        }

        @Override
        public JavaExpression visit(DoubleLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.DOUBLE), expr.asDouble());
        }

        @Override
        public JavaExpression visit(StringLiteralExpr expr, JavaExpressionContext context) {
            TypeMirror stringTM =
                    TypesUtils.typeFromClass(String.class, types, env.getElementUtils());
            return new ValueLiteral(stringTM, expr.asString());
        }

        @Override
        public JavaExpression visit(BooleanLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.BOOLEAN), expr.getValue());
        }

        @Override
        public JavaExpression visit(ThisExpr n, JavaExpressionContext context) {
            if (context.receiver == null) {
                return null;
            }
            if (!context.receiver.containsUnknown()) {
                // "this" is the receiver of the context
                return context.receiver;
            }
            return new ThisReference(context.receiver.getType());
        }

        @Override
        public JavaExpression visit(SuperExpr n, JavaExpressionContext context) {
            // super literal
            TypeMirror superclass = TypesUtils.getSuperclass(context.receiver.getType(), types);
            if (superclass == null) {
                throw new ParseRuntimeException(
                        constructParserException("super", "super class not found"));
            }
            return new ThisReference(superclass);
        }

        // expr is an expression in parentheses.
        @Override
        public JavaExpression visit(EnclosedExpr expr, JavaExpressionContext context) {
            return expr.getInner().accept(this, context);
        }

        @Override
        public JavaExpression visit(ArrayAccessExpr expr, JavaExpressionContext context) {
            JavaExpression array = expr.getName().accept(this, context);
            TypeMirror arrayType = array.getType();
            if (arrayType.getKind() != TypeKind.ARRAY) {
                throw new ParseRuntimeException(
                        constructParserException(
                                expr.toString(),
                                String.format(
                                        "expected an array, found %s of type %s [%s]",
                                        array, arrayType, arrayType.getKind())));
            }
            TypeMirror componentType = ((ArrayType) arrayType).getComponentType();

            JavaExpression index = expr.getIndex().accept(this, context);

            return new ArrayAccess(componentType, array, index);
        }

        // expr is an identifier with no dots in its name.
        @Override
        public JavaExpression visit(NameExpr expr, JavaExpressionContext context) {
            String s = expr.getNameAsString();
            setResolverField();

            // Formal parameter, using "#2" syntax.
            if (!context.parsingMember && s.startsWith(PARAMETER_REPLACEMENT)) {
                // A parameter is a local variable, but it can be referenced outside of local scope
                // (at the method scope) using the special #NN syntax.
                return getParameterJavaExpression(s, context);
            }

            // Local variable, parameter, or field.
            if (!context.parsingMember && context.useLocalScope) {
                // Attempt to match a local variable within the scope of the
                // given path before attempting to match a field.
                VariableElement varElem =
                        resolver.findLocalVariableOrParameterOrField(s, annotatedConstruct);
                if (varElem != null) {
                    if (varElem.getKind() == ElementKind.FIELD) {
                        boolean isOriginalReceiver = context.receiver instanceof ThisReference;
                        return getFieldJavaExpression(varElem, context, isOriginalReceiver);
                    } else {
                        return new LocalVariable(varElem);
                    }
                }
            }

            // Field access
            TypeMirror receiverType = context.receiver.getType();
            // originalReceiver is true if receiverType has not been reassigned.
            boolean originalReceiver = true;
            VariableElement fieldElem = null;
            if (s.equals("length") && receiverType.getKind() == TypeKind.ARRAY) {
                fieldElem = resolver.findField(s, receiverType, annotatedConstruct);
            }
            if (fieldElem == null) {
                // Search for field in each enclosing class.
                while (receiverType.getKind() == TypeKind.DECLARED) {
                    fieldElem = resolver.findField(s, receiverType, annotatedConstruct);
                    if (fieldElem != null) {
                        break;
                    }
                    receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
                    originalReceiver = false;
                }
            }
            if (fieldElem != null && fieldElem.getKind() == ElementKind.FIELD) {
                FieldAccess fieldAccess =
                        (FieldAccess) getFieldJavaExpression(fieldElem, context, originalReceiver);
                TypeElement scopeClassElement =
                        TypesUtils.getTypeElement(fieldAccess.getReceiver().getType());
                if (!originalReceiver
                        && !ElementUtils.isStatic(fieldElem)
                        && ElementUtils.isStatic(scopeClassElement)) {
                    throw new ParseRuntimeException(
                            constructParserException(
                                    s,
                                    "a non-static field can't be referenced from a static inner class or enum"));
                }
                return fieldAccess;
            }

            // Class name
            Element classElem = resolver.findClass(s, annotatedConstruct);
            TypeMirror classType = ElementUtils.getType(classElem);
            if (classType != null) {
                return new ClassName(classType);
            }

            // Err if a formal parameter name is used, instead of the "#2" syntax.
            MethodTree enclMethod = TreePathUtil.enclosingMethod(annotatedConstruct);
            if (enclMethod != null) {
                List<? extends VariableTree> params = enclMethod.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    if (params.get(i).getName().contentEquals(s)) {
                        throw new ParseRuntimeException(
                                constructParserException(
                                        s,
                                        String.format(
                                                DependentTypesError.FORMAL_PARAM_NAME_STRING,
                                                i + 1,
                                                s)));
                    }
                }
            }

            throw new ParseRuntimeException(constructParserException(s, "identifier not found"));
        }

        @Override
        public JavaExpression visit(MethodCallExpr expr, JavaExpressionContext context) {
            setResolverField();

            /// TODO: ***** Should only the method name use the changed scope?  I don't see why
            /// arguments should use a different parsing context, and this might be buggy if
            /// arguments coincidentally had the same name as a formal parameter.  A Java expression
            /// can be with respect to its local scope.

            // `expr` is a method call.  If it has scope (a receiver expression), change the parsing
            // context so that identifiers are resolved with respect to the receiver.
            if (expr.getScope().isPresent()) {
                JavaExpression receiver = expr.getScope().get().accept(this, context);
                context = context.copyChangeToParsingMemberOfReceiver(receiver);
                expr = expr.removeScope();
            }

            String methodName = expr.getNameAsString();

            // parse argument list
            List<JavaExpression> arguments = new ArrayList<>();
            for (Expression argument : expr.getArguments()) {
                arguments.add(argument.accept(this, context));
            }

            ExecutableElement methodElement;
            try {
                methodElement =
                        getMethodElement(
                                methodName,
                                context.receiver.getType(),
                                annotatedConstruct,
                                arguments,
                                resolver);

                // Box any arguments that require it.
                for (int i = 0; i < arguments.size(); i++) {
                    VariableElement parameter = methodElement.getParameters().get(i);
                    TypeMirror parameterType = parameter.asType();
                    JavaExpression argument = arguments.get(i);
                    TypeMirror argumentType = argument.getType();
                    // boxing necessary
                    if (TypesUtils.isBoxedPrimitive(parameterType)
                            && TypesUtils.isPrimitive(argumentType)) {
                        MethodSymbol valueOfMethod =
                                TreeBuilder.getValueOfMethod(env, parameterType);
                        List<JavaExpression> p = new ArrayList<>();
                        p.add(argument);
                        JavaExpression boxedParam =
                                new MethodCall(
                                        parameterType,
                                        valueOfMethod,
                                        new ClassName(parameterType),
                                        p);
                        arguments.set(i, boxedParam);
                    }
                }
            } catch (Throwable t) {
                if (t.getMessage() == null) {
                    throw new BugInCF("no detail message in " + t.getClass(), t);
                }
                throw new ParseRuntimeException(
                        constructParserException(expr.toString(), t.getMessage()));
            }

            // TODO: reinstate this test, but issue a warning that the user
            // can override, rather than halting parsing which the user cannot override.
            /*if (!PurityUtils.isDeterministic(context.checkerContext.getAnnotationProvider(),
                    methodElement)) {
                throw new JavaExpressionParseException(new DiagMessage(ERROR,
                        "flowexpr.method.not.deterministic",
                        methodElement.getSimpleName()));
            }*/
            if (ElementUtils.isStatic(methodElement)) {
                Element classElem = methodElement.getEnclosingElement();
                JavaExpression staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
                return new MethodCall(
                        ElementUtils.getType(methodElement),
                        methodElement,
                        staticClassReceiver,
                        arguments);
            } else {
                if (context.receiver instanceof ClassName) {
                    throw new ParseRuntimeException(
                            constructParserException(
                                    expr.toString(),
                                    "a non-static method call cannot have a class name as a receiver"));
                }
                TypeMirror methodType =
                        TypesUtils.substituteMethodReturnType(
                                methodElement, context.receiver.getType(), env);
                return new MethodCall(methodType, methodElement, context.receiver, arguments);
            }
        }

        /**
         * Returns the ExecutableElement for a method, or throws an exception.
         *
         * @param methodName the method name
         * @param receiverType the receiver type
         * @param path the path
         * @param arguments the arguments
         * @param resolver the resolver
         * @return the ExecutableElement for a method, or throws an exception
         * @throws JavaExpressionParseException if the string cannot be parsed as a method name
         */
        private ExecutableElement getMethodElement(
                String methodName,
                TypeMirror receiverType,
                TreePath path,
                List<JavaExpression> arguments,
                Resolver resolver)
                throws JavaExpressionParseException {

            List<TypeMirror> argumentTypes =
                    CollectionsPlume.mapList(JavaExpression::getType, arguments);

            Element element = null;

            if (receiverType.getKind() == TypeKind.ARRAY) {
                element = resolver.findMethod(methodName, receiverType, path, argumentTypes);
            }

            // Search for method in each enclosing class.
            if (element == null) {
                while (receiverType.getKind() == TypeKind.DECLARED) {
                    element = resolver.findMethod(methodName, receiverType, path, argumentTypes);
                    if (element.getKind() == ElementKind.METHOD) {
                        break;
                    }
                    receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
                }
            }

            if (element == null) {
                throw constructParserException(methodName, "no such method");
            }
            if (element.getKind() != ElementKind.METHOD) {
                throw constructParserException(
                        methodName, "not a method, but a " + element.getKind());
            }

            return (ExecutableElement) element;
        }

        // expr is a field access, a fully qualified class name, or a class name qualified with
        // another class name (e.g. {@code OuterClass.InnerClass})
        @Override
        public JavaExpression visit(FieldAccessExpr expr, JavaExpressionContext context) {
            setResolverField();

            Symbol.PackageSymbol packageSymbol =
                    resolver.findPackage(expr.getScope().toString(), annotatedConstruct);
            if (packageSymbol != null) {
                ClassSymbol classSymbol =
                        resolver.findClassInPackage(
                                expr.getNameAsString(), packageSymbol, annotatedConstruct);
                if (classSymbol != null) {
                    return new ClassName(classSymbol.asType());
                }
                throw new ParseRuntimeException(
                        constructParserException(
                                expr.toString(),
                                "could not find class "
                                        + expr.getNameAsString()
                                        + " inside "
                                        + expr.getScope().toString()));
            }

            JavaExpression receiver = expr.getScope().accept(this, context);

            // Parse the rest, with a new receiver.
            JavaExpressionContext newContext =
                    context.copyChangeToParsingMemberOfReceiver(receiver);
            return visit(expr.getNameAsExpression(), newContext);
        }

        // expr is a Class literal
        @Override
        public JavaExpression visit(ClassExpr expr, JavaExpressionContext context) {
            TypeMirror result = convertTypeToTypeMirror(expr.getType(), context);
            if (result == null) {
                throw new ParseRuntimeException(
                        constructParserException(
                                expr.toString(), "is an unparsable class literal"));
            }
            return new ClassName(result);
        }

        @Override
        public JavaExpression visit(ArrayCreationExpr expr, JavaExpressionContext context) {
            List<JavaExpression> dimensions = new ArrayList<>();
            for (ArrayCreationLevel dimension : expr.getLevels()) {
                if (dimension.getDimension().isPresent()) {
                    dimensions.add(dimension.getDimension().get().accept(this, context));
                } else {
                    dimensions.add(null);
                }
            }

            List<JavaExpression> initializers = new ArrayList<>();
            if (expr.getInitializer().isPresent()) {
                for (Expression initializer : expr.getInitializer().get().getValues()) {
                    initializers.add(initializer.accept(this, context));
                }
            }
            TypeMirror arrayType = convertTypeToTypeMirror(expr.getElementType(), context);
            if (arrayType == null) {
                throw new ParseRuntimeException(
                        constructParserException(
                                expr.getElementType().asString(), "type not parsable"));
            }
            for (int i = 0; i < dimensions.size(); i++) {
                arrayType = TypesUtils.createArrayType(arrayType, env.getTypeUtils());
            }
            return new ArrayCreation(arrayType, dimensions, initializers);
        }

        /**
         * Converts the JavaParser type to a TypeMirror. Returns null if {@code type}'s kind is not
         * handled.
         *
         * @param type a JavaParser type
         * @param context a JavaExpressionContext
         * @return a TypeMirror corresponding to {@code type}, or null if {@code type} isn't handled
         */
        private @Nullable TypeMirror convertTypeToTypeMirror(
                Type type, JavaExpressionContext context) {
            if (type.isClassOrInterfaceType()) {
                return StaticJavaParser.parseExpression(type.asString())
                        .accept(this, context)
                        .getType();
            } else if (type.isPrimitiveType()) {
                switch (type.asPrimitiveType().getType()) {
                    case BOOLEAN:
                        return types.getPrimitiveType(TypeKind.BOOLEAN);
                    case BYTE:
                        return types.getPrimitiveType(TypeKind.BYTE);
                    case SHORT:
                        return types.getPrimitiveType(TypeKind.SHORT);
                    case INT:
                        return types.getPrimitiveType(TypeKind.INT);
                    case CHAR:
                        return types.getPrimitiveType(TypeKind.CHAR);
                    case FLOAT:
                        return types.getPrimitiveType(TypeKind.FLOAT);
                    case LONG:
                        return types.getPrimitiveType(TypeKind.LONG);
                    case DOUBLE:
                        return types.getPrimitiveType(TypeKind.DOUBLE);
                }
            } else if (type.isVoidType()) {
                return types.getNoType(TypeKind.VOID);
            } else if (type.isArrayType()) {
                return types.getArrayType(
                        convertTypeToTypeMirror(type.asArrayType().getComponentType(), context));
            }
            return null;
        }

        /**
         * Returns a JavaExpression for the given field name.
         *
         * @param fieldElem the field
         * @param context the context
         * @param originalReceiver whether the receiver is the original one
         * @return a JavaExpression for the given name
         */
        private static JavaExpression getFieldJavaExpression(
                VariableElement fieldElem,
                JavaExpressionContext context,
                boolean originalReceiver) {
            TypeMirror receiverType = context.receiver.getType();

            TypeMirror fieldType = ElementUtils.getType(fieldElem);
            if (ElementUtils.isStatic(fieldElem)) {
                Element classElem = fieldElem.getEnclosingElement();
                JavaExpression staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
                return new FieldAccess(staticClassReceiver, fieldType, fieldElem);
            }
            JavaExpression locationOfField;
            if (originalReceiver) {
                locationOfField = context.receiver;
            } else {
                locationOfField =
                        JavaExpression.fromNode(
                                context.checkerContext.getAnnotationProvider(),
                                new ImplicitThisNode(receiverType));
            }
            if (locationOfField instanceof ClassName) {
                throw new ParseRuntimeException(
                        constructParserException(
                                fieldElem.getSimpleName().toString(),
                                "a non-static field cannot have a class name as a receiver."));
            }
            return new FieldAccess(locationOfField, fieldType, fieldElem);
        }

        /**
         * Returns a JavaExpression for the given parameter; that is, returns an element of {@code
         * context.arguments}.
         *
         * @param s a String that starts with PARAMETER_REPLACEMENT
         * @param context the context
         * @return the JavaExpression for the given parameter
         */
        private static JavaExpression getParameterJavaExpression(
                String s, JavaExpressionContext context) {
            if (context.arguments == null) {
                throw new ParseRuntimeException(constructParserException(s, "no parameter found"));
            }
            int idx = Integer.parseInt(s.substring(PARAMETER_REPLACEMENT_LENGTH));

            if (idx == 0) {
                throw new ParseRuntimeException(
                        constructParserException(
                                "#0",
                                "use \"this\" for the receiver or \"#1\" for the first formal parameter"));
            }
            if (idx > context.arguments.size()) {
                throw new ParseRuntimeException(
                        new JavaExpressionParseException(
                                "flowexpr.parse.index.too.big", Integer.toString(idx)));
            }
            return context.arguments.get(idx - 1);
        }
    }

    /**
     * Returns a list of 1-based indices of all formal parameters that occur in {@code s}. Each
     * formal parameter occurs in s as a string like "#1" or "#4". This routine does not do proper
     * parsing; for instance, if "#2" appears within a string in s, then 2 would be in the result
     * list.
     *
     * @param s a Java expression
     * @return a list of 1-based indices of all formal parameters that occur in {@code s}
     */
    public static List<Integer> parameterIndices(String s) {
        List<Integer> result = new ArrayList<>();
        Matcher matcher = UNANCHORED_PARAMETER_PATTERN.matcher(s);
        while (matcher.find()) {
            int idx = Integer.parseInt(matcher.group(1));
            result.add(idx);
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Contexts
    ///

    /**
     * Context used to parse a Java expression. When parsing expression E in annotation
     * {@code @A(E)}, the context is the program element that is annotated by {@code @A(E)}.
     */
    public static class JavaExpressionContext {
        public final JavaExpression receiver;
        /**
         * In a context for a method declaration or lambda, the formals. In a context for a method
         * invocation, the actuals. In other contexts, an empty list.
         */
        public final List<JavaExpression> arguments;

        public final BaseContext checkerContext;
        /**
         * Whether or not the FlowExpressionParser is parsing the "member" part of a member select.
         * If so, certain constructs like "#2" and local variables cannot occur.
         */
        public final boolean parsingMember;
        /** Whether the TreePath should be used to find identifiers. Defaults to true. */
        public final boolean useLocalScope;

        /**
         * Creates a context for parsing a Java expression.
         *
         * @param receiver used to replace "this" in a Java expression and used to resolve
         *     identifiers in any Java expression with an implicit "this"
         * @param arguments used to replace parameter references, e.g. #1, in Java expressions, null
         *     if no arguments
         * @param checkerContext used to create {@link
         *     org.checkerframework.dataflow.expression.JavaExpression}s
         */
        public JavaExpressionContext(
                JavaExpression receiver,
                List<JavaExpression> arguments,
                BaseContext checkerContext) {
            this(receiver, arguments, checkerContext, false, true);
        }

        private JavaExpressionContext(
                JavaExpression receiver,
                List<JavaExpression> arguments,
                BaseContext checkerContext,
                boolean parsingMember,
                boolean useLocalScope) {
            assert checkerContext != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.checkerContext = checkerContext;
            this.parsingMember = parsingMember;
            this.useLocalScope = useLocalScope;
        }

        /**
         * Creates a {@link JavaExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used to translate parameter numbers in a Java expression to
         *     formal parameters of the method
         * @param enclosingTree used to look up fields and as the type of "this" in Java expressions
         * @param checkerContext used to build JavaExpression
         * @return context created from {@code methodDeclaration}
         */
        public static JavaExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, Tree enclosingTree, BaseContext checkerContext) {
            return buildContextForMethodDeclaration(
                    methodDeclaration, TreeUtils.typeOf(enclosingTree), checkerContext);
        }

        /**
         * Creates a {@link JavaExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used to translate parameter numbers in a Java expression to
         *     formal parameters of the method
         * @param currentPath the path to the method. It is used to find the enclosing class, which
         *     is used to look up fields and as the type of "this" in Java expressions.
         * @param checkerContext used to build JavaExpression
         * @return context created from {@code methodDeclaration}
         */
        public static JavaExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, TreePath currentPath, BaseContext checkerContext) {
            Tree classTree = TreePathUtil.enclosingClass(currentPath);
            return buildContextForMethodDeclaration(methodDeclaration, classTree, checkerContext);
        }

        /**
         * Creates a {@link JavaExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used to translate parameter numbers in a Java expression to
         *     formal parameters of the method
         * @param enclosingType used to look up fields and as type of "this" in Java expressions
         * @param checkerContext used to build JavaExpression
         * @return context created from {@code methodDeclaration}
         */
        public static JavaExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration,
                TypeMirror enclosingType,
                BaseContext checkerContext) {

            Node receiver;
            if (methodDeclaration.getModifiers().getFlags().contains(Modifier.STATIC)) {
                Element classElt =
                        ElementUtils.enclosingClass(
                                TreeUtils.elementFromDeclaration(methodDeclaration));
                receiver = new ClassNameNode(enclosingType, classElt);
            } else {
                receiver = new ImplicitThisNode(enclosingType);
            }
            JavaExpression receiverJe =
                    JavaExpression.fromNode(checkerContext.getAnnotationProvider(), receiver);
            List<JavaExpression> argumentsJe = new ArrayList<>();
            for (VariableTree arg : methodDeclaration.getParameters()) {
                argumentsJe.add(
                        JavaExpression.fromNode(
                                checkerContext.getAnnotationProvider(),
                                new LocalVariableNode(arg, receiver)));
            }
            return new JavaExpressionContext(receiverJe, argumentsJe, checkerContext);
        }

        /**
         * Creates a {@link JavaExpressionContext} for the given lambda.
         *
         * @param lambdaTree a lambda
         * @param path the path to the lambda
         * @param checkerContext used to build JavaExpression
         * @return context created for {@code lambdaTree}
         */
        public static JavaExpressionContext buildContextForLambda(
                LambdaExpressionTree lambdaTree, TreePath path, BaseContext checkerContext) {
            TypeMirror enclosingType = TreeUtils.typeOf(TreePathUtil.enclosingClass(path));
            Node receiver = new ImplicitThisNode(enclosingType);
            JavaExpression receiverJe =
                    JavaExpression.fromNode(checkerContext.getAnnotationProvider(), receiver);
            List<JavaExpression> argumentsJe = new ArrayList<>();
            for (VariableTree arg : lambdaTree.getParameters()) {
                argumentsJe.add(
                        JavaExpression.fromNode(
                                checkerContext.getAnnotationProvider(),
                                new LocalVariableNode(arg, receiver)));
            }
            return new JavaExpressionContext(receiverJe, argumentsJe, checkerContext);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the class {@code classTree} as seen at the
         * class declaration.
         *
         * @param classTree a class
         * @param checkerContext used to build JavaExpression
         * @return a {@link JavaExpressionContext} for the class {@code classTree} as seen at the
         *     class declaration
         */
        public static JavaExpressionContext buildContextForClassDeclaration(
                ClassTree classTree, BaseContext checkerContext) {
            Node receiver = new ImplicitThisNode(TreeUtils.typeOf(classTree));

            JavaExpression receiverJe =
                    JavaExpression.fromNode(checkerContext.getAnnotationProvider(), receiver);
            return new JavaExpressionContext(receiverJe, Collections.emptyList(), checkerContext);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the method called by {@code
         * methodInvocation}, as seen at the method use (i.e., at the call site).
         *
         * @param methodInvocation a method invocation
         * @param checkerContext the javac components to use
         * @return a {@link JavaExpressionContext} for the method {@code methodInvocation}
         */
        public static JavaExpressionContext buildContextForMethodUse(
                MethodInvocationNode methodInvocation, BaseContext checkerContext) {
            Node receiver = methodInvocation.getTarget().getReceiver();
            JavaExpression receiverJe =
                    JavaExpression.fromNode(checkerContext.getAnnotationProvider(), receiver);
            List<JavaExpression> argumentsJe = new ArrayList<>();
            for (Node arg : methodInvocation.getArguments()) {
                argumentsJe.add(
                        JavaExpression.fromNode(checkerContext.getAnnotationProvider(), arg));
            }
            return new JavaExpressionContext(receiverJe, argumentsJe, checkerContext);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the method called by {@code
         * methodInvocation}, as seen at the method use (i.e., at the call site).
         *
         * @param methodInvocation a method invocation
         * @param checkerContext the javac components to use
         * @return a {@link JavaExpressionContext} for the method {@code methodInvocation}
         */
        public static JavaExpressionContext buildContextForMethodUse(
                MethodInvocationTree methodInvocation, BaseContext checkerContext) {
            JavaExpression receiverJe =
                    JavaExpression.getReceiver(
                            methodInvocation, checkerContext.getAnnotationProvider());

            List<? extends ExpressionTree> args = methodInvocation.getArguments();
            List<JavaExpression> argumentsJe = new ArrayList<>(args.size());
            for (ExpressionTree argTree : args) {
                argumentsJe.add(
                        JavaExpression.fromTree(checkerContext.getAnnotationProvider(), argTree));
            }

            return new JavaExpressionContext(receiverJe, argumentsJe, checkerContext);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the constructor {@code n} (represented as a
         * {@link Node} as seen at the constructor use (i.e., at "new" expression).
         *
         * @param n an object creation node
         * @param checkerContext the checker context
         * @return a {@link JavaExpressionContext} for the constructor {@code n} (represented as a
         *     {@link Node} as seen at the constructor use (i.e., at "new" expression)
         */
        public static JavaExpressionContext buildContextForNewClassUse(
                ObjectCreationNode n, BaseContext checkerContext) {

            // This returns an Unknown with the type set to the class in which the
            // constructor is declared
            JavaExpression receiverJe =
                    JavaExpression.fromNode(checkerContext.getAnnotationProvider(), n);

            List<JavaExpression> argumentsJe = new ArrayList<>();
            for (Node arg : n.getArguments()) {
                argumentsJe.add(
                        JavaExpression.fromNode(checkerContext.getAnnotationProvider(), arg));
            }

            return new JavaExpressionContext(receiverJe, argumentsJe, checkerContext);
        }

        /**
         * Returns a copy of the context that differs in that it has a different receiver and
         * parsingMember is set to true. The outer receiver remains unchanged.
         */
        public JavaExpressionContext copyChangeToParsingMemberOfReceiver(JavaExpression receiver) {
            return new JavaExpressionContext(
                    receiver, arguments, checkerContext, /*parsingMember=*/ true, useLocalScope);
        }

        /**
         * Returns a copy of the context that differs in that useLocalScope is set to the given
         * value.
         */
        public JavaExpressionContext copyAndSetUseLocalScope(boolean useLocalScope) {
            return new JavaExpressionContext(
                    receiver, arguments, checkerContext, parsingMember, useLocalScope);
        }

        /**
         * Format this object verbosely, on multiple lines but without a trailing newline.
         *
         * @return a verbose string representation of this
         */
        public String toStringDebug() {
            StringJoiner sj = new StringJoiner(System.lineSeparator() + "  ");
            sj.add("JavaExpressionContext:");
            sj.add("receiver=" + receiver.toStringDebug());
            sj.add("arguments=" + arguments);
            sj.add("checkerContext=" + "...");
            // sj.add("checkerContext="+ checkerContext);
            sj.add("parsingMember=" + parsingMember);
            sj.add("useLocalScope=" + useLocalScope);
            return sj.toString();
        }
    }

    /**
     * Returns the type of the innermost enclosing class. Returns Type.noType if no enclosing class
     * is found.
     *
     * <p>If the innermost enclosing class is static, this method returns the type of that class. By
     * contrast, {@link DeclaredType#getEnclosingType()} returns the type of the innermost enclosing
     * class that is not static.
     *
     * @param type a DeclaredType
     * @return the type of the innermost enclosing class or Type.noType
     */
    private static TypeMirror getTypeOfEnclosingClass(DeclaredType type) {
        if (type instanceof ClassType) {
            // enclClass() needs to be called on tsym.owner,
            // otherwise it simply returns tsym.
            Symbol sym = ((ClassType) type).tsym.owner;

            if (sym == null) {
                return com.sun.tools.javac.code.Type.noType;
            }

            ClassSymbol cs = sym.enclClass();

            if (cs == null) {
                return com.sun.tools.javac.code.Type.noType;
            }

            return cs.asType();
        } else {
            return type.getEnclosingType();
        }
    }

    public static JavaExpression fromVariableTree(AnnotatedTypeFactory provider, VariableTree tree)
            throws JavaExpressionParseException {
        Element elt = TreeUtils.elementFromDeclaration(tree);

        if (elt.getKind() == ElementKind.LOCAL_VARIABLE
                || elt.getKind() == ElementKind.RESOURCE_VARIABLE
                || elt.getKind() == ElementKind.EXCEPTION_PARAMETER
                || elt.getKind() == ElementKind.PARAMETER) {
            return new LocalVariable(elt);
        }
        JavaExpression receiverJe = JavaExpression.getImplicitReceiver(elt);
        JavaExpressionContext context =
                new JavaExpressionContext(receiverJe, /*arguments=*/ null, provider.getContext());
        return parse(
                tree.getName().toString(),
                context,
                provider.getPath(tree),
                /*useLocalScope=*/ false);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Exceptions
    ///

    /**
     * An exception that indicates a parse error. Call {@link #getDiagMessage} to obtain a {@link
     * DiagMessage} that can be used for error reporting.
     */
    public static class JavaExpressionParseException extends Exception {
        private static final long serialVersionUID = 2L;
        private @CompilerMessageKey String errorKey;
        public final Object[] args;

        public JavaExpressionParseException(@CompilerMessageKey String errorKey, Object... args) {
            this(null, errorKey, args);
        }

        public JavaExpressionParseException(
                Throwable cause, @CompilerMessageKey String errorKey, Object... args) {
            super(cause);
            this.errorKey = errorKey;
            this.args = args;
        }

        @Override
        public String getMessage() {
            return errorKey + " " + Arrays.toString(args);
        }

        /**
         * Return a DiagMessage that can be used for error reporting.
         *
         * @return a DiagMessage that can be used for error reporting
         */
        public DiagMessage getDiagMessage() {
            return new DiagMessage(Kind.ERROR, errorKey, args);
        }

        public boolean isFlowParseError() {
            return errorKey.endsWith("flowexpr.parse.error");
        }
    }

    /**
     * Returns a {@link JavaExpressionParseException} for the expression {@code expr} with
     * explanation {@code explanation}.
     */
    private static JavaExpressionParseException constructParserException(
            String expr, String explanation) {
        if (expr == null) {
            throw new Error("Must have an expression.");
        }
        if (explanation == null) {
            throw new Error("Must have an explanation.");
        }
        return new JavaExpressionParseException(
                (Throwable) null,
                "flowexpr.parse.error",
                "Invalid '" + expr + "' because " + explanation);
    }

    /**
     * The Runtime equivalent of {@link JavaExpressionParseException}. This class is needed to wrap
     * this exception into an unchecked exception.
     */
    private static class ParseRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 2L;
        private final JavaExpressionParseException exception;

        private ParseRuntimeException(JavaExpressionParseException exception) {
            this.exception = exception;
        }

        private JavaExpressionParseException getCheckedException() {
            return exception;
        }
    }
}
