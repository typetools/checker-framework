package org.checkerframework.framework.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayCreation;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.analysis.FlowExpressions.ValueLiteral;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

/**
 * A collection of helper methods to parse a string that represents a restricted Java expression.
 *
 * @checker_framework.manual #java-expressions-as-arguments Writing Java expressions as annotation
 *     arguments
 * @checker_framework.manual #dependent-types Annotations whose argument is a Java expression
 *     (dependent type annotations)
 */
public class FlowExpressionParseUtil {

    /** Regular expression for a formal parameter use. */
    protected static final String PARAMETER_REGEX = "#([1-9][0-9]*)";

    /** Anchored pattern for a formal parameter. */
    protected static final Pattern ANCHORED_PARAMETER_PATTERN =
            Pattern.compile("^" + PARAMETER_REGEX + "$");

    /** Unanchored; can be used to find all formal parameter uses. */
    protected static final Pattern UNANCHORED_PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEX);

    /** Parsable replacement for parameter references. */
    private static final String PARMETER_REPLACEMENT = "_param_";

    private static final int PARAMETER_REPLACEMENT_LENGTH = PARMETER_REPLACEMENT.length();

    /**
     * Parse a string and return its representation as a {@link Receiver}, or throw an {@link
     * FlowExpressionParseException}.
     *
     * @param expression flow expression to parse
     * @param context information about any receiver and arguments
     * @param localScope path to local scope to use
     * @param useLocalScope whether {@code localScope} should be used to resolve identifiers
     */
    public static Receiver parse(
            String expression,
            FlowExpressionContext context,
            TreePath localScope,
            boolean useLocalScope)
            throws FlowExpressionParseException {
        context = context.copyAndSetUseLocalScope(useLocalScope);
        ProcessingEnvironment env = context.checkerContext.getProcessingEnvironment();
        Expression expr;
        try {
            expr = StaticJavaParser.parseExpression(replaceParameterSyntax(expression));
        } catch (ParseProblemException e) {
            throw constructParserException(expression, "is an invalid expression");
        }

        Receiver result;
        try {
            result = expr.accept(new ExpressionToReceiverVisitor(localScope, env), context);
        } catch (ParseRuntimeException e) {
            // The visitors can't throw exceptions because they need to override the methods in the
            // superclass.
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
                            "a class name cannot terminate a flow expression string, where result=%s [%s]",
                            result, result.getClass()));
        }
        return result;
    }

    /**
     * Replaces every occurrence of "#(number)" with "PARAMETER_REPLACEMENT(number)" where number is
     * an index of a parameter.
     */
    private static String replaceParameterSyntax(String expression) {
        String updatedExpression = expression;

        for (Integer integer : parameterIndices(expression)) {
            updatedExpression =
                    updatedExpression.replaceAll("#" + integer, PARMETER_REPLACEMENT + integer);
        }

        return updatedExpression;
    }

    /**
     * A visitor class that converts a JavaParser {@link Expression} to a {@link
     * FlowExpressions.Receiver}.
     */
    private static class ExpressionToReceiverVisitor
            extends GenericVisitorWithDefaults<Receiver, FlowExpressionContext> {

        private final TreePath path;
        private final ProcessingEnvironment env;
        private final Types types;

        ExpressionToReceiverVisitor(TreePath path, ProcessingEnvironment env) {
            this.path = path;
            this.env = env;
            this.types = env.getTypeUtils();
        }

        /** If the expression is not supported, throw a {@link ParseRuntimeException} by default. */
        @Override
        public Receiver defaultAction(
                com.github.javaparser.ast.Node n, FlowExpressionContext context) {
            String message = "is not a supported expression";
            if (context.parsingMember) {
                message += " in a context with parsingMember=true";
            }
            throw new ParseRuntimeException(constructParserException(n.toString(), message));
        }

        @Override
        public Receiver visit(NullLiteralExpr expr, FlowExpressionContext context) {
            return new ValueLiteral(types.getNullType(), (Object) null);
        }

        @Override
        public Receiver visit(IntegerLiteralExpr expr, FlowExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.INT), expr.asNumber());
        }

        @Override
        public Receiver visit(LongLiteralExpr expr, FlowExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.LONG), expr.asNumber());
        }

        @Override
        public Receiver visit(CharLiteralExpr expr, FlowExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.CHAR), expr.asChar());
        }

        @Override
        public Receiver visit(DoubleLiteralExpr expr, FlowExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.DOUBLE), expr.asDouble());
        }

        @Override
        public Receiver visit(StringLiteralExpr expr, FlowExpressionContext context) {
            TypeMirror stringTM =
                    TypesUtils.typeFromClass(String.class, types, env.getElementUtils());
            return new ValueLiteral(stringTM, expr.asString());
        }

        /**
         * Returns the receiver, {@link FlowExpressionContext#receiver}, of the context.
         *
         * @return the receiver, {@link FlowExpressionContext#receiver}, of the context
         */
        @Override
        public Receiver visit(ThisExpr n, FlowExpressionContext context) {
            if (context.receiver != null && !context.receiver.containsUnknown()) {
                // "this" is the receiver of the context
                return context.receiver;
            }
            return new ThisReference(context.receiver == null ? null : context.receiver.getType());
        }

        /**
         * Returns the receiver of the superclass of the context.
         *
         * @return the receiver of the superclass of the context
         */
        @Override
        public Receiver visit(SuperExpr n, FlowExpressionContext context) {
            // super literal
            List<? extends TypeMirror> superTypes =
                    types.directSupertypes(context.receiver.getType());
            // find class supertype
            for (TypeMirror t : superTypes) {
                // ignore interface types
                if (!(t instanceof ClassType)) {
                    continue;
                }
                ClassType tt = (ClassType) t;
                if (!tt.isInterface()) {
                    return new ThisReference(t);
                }
            }

            throw new ParseRuntimeException(
                    constructParserException("super", "super class not found"));
        }

        /** @param expr an expression in parentheses. */
        @Override
        public Receiver visit(EnclosedExpr expr, FlowExpressionContext context) {
            return expr.getInner().accept(this, context);
        }

        /**
         * Returns the receiver of an array access.
         *
         * @return the receiver of an array access
         */
        @Override
        public Receiver visit(ArrayAccessExpr expr, FlowExpressionContext context) {
            Receiver array = expr.getName().accept(this, context);
            FlowExpressionContext contextForIndex = context.copyAndUseOuterReceiver();
            Receiver index = expr.getIndex().accept(this, contextForIndex);

            TypeMirror arrayType = array.getType();
            if (arrayType.getKind() != TypeKind.ARRAY) {
                throw new ParseRuntimeException(
                        constructParserException(
                                expr.toString(),
                                String.format("array not an array: %s : %s", array, arrayType)));
            }

            TypeMirror componentType = ((ArrayType) arrayType).getComponentType();
            return new ArrayAccess(componentType, array, index);
        }

        /** @param expr a unique identifier with no dots in its name. */
        @Override
        public Receiver visit(NameExpr expr, FlowExpressionContext context) {
            String s = expr.getNameAsString();
            Resolver resolver = new Resolver(env);
            if (!context.parsingMember && s.startsWith(PARMETER_REPLACEMENT)) {
                // A parameter is a local variable, but it can be referenced outside of local scope
                // using the special #NN syntax.
                return getParameterReceiver(s, context);
            } else if (!context.parsingMember && context.useLocalScope) {
                // Attempt to match a local variable within the scope of the
                // given path before attempting to match a field.
                VariableElement varElem = resolver.findLocalVariableOrParameterOrField(s, path);
                if (varElem != null) {
                    if (varElem.getKind() == ElementKind.FIELD) {
                        boolean isOriginalReceiver = context.receiver instanceof ThisReference;
                        return getReceiverField(s, context, isOriginalReceiver, varElem);
                    } else {
                        return new LocalVariable(varElem);
                    }
                }
            }

            // field access
            TypeMirror receiverType = context.receiver.getType();
            boolean originalReceiver = true;
            VariableElement fieldElem = null;

            if (receiverType.getKind() == TypeKind.ARRAY && s.equals("length")) {
                fieldElem = resolver.findField(s, receiverType, path);
            }

            // Search for field in each enclosing class.
            while (receiverType.getKind() == TypeKind.DECLARED) {
                fieldElem = resolver.findField(s, receiverType, path);
                if (fieldElem != null) {
                    break;
                }
                receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
                originalReceiver = false;
            }

            if (fieldElem != null && fieldElem.getKind() == ElementKind.FIELD) {
                FieldAccess fieldAccess =
                        (FieldAccess) getReceiverField(s, context, originalReceiver, fieldElem);
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
            Element classElem = resolver.findClass(s, path);
            TypeMirror classType = ElementUtils.getType(classElem);
            if (classType != null) {
                return new ClassName(classType);
            }

            MethodTree enclMethod = TreeUtils.enclosingMethod(path);
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

        /** @param expr a method call with or without a receiver expressions. */
        @Override
        public Receiver visit(MethodCallExpr expr, FlowExpressionContext context) {
            String s = expr.toString();
            Resolver resolver = new Resolver(env);

            // methods with scope (receiver expression) need to change the parsing context so that
            // identifiers are resolved with respect to the receiver.
            if (expr.getScope().isPresent()) {
                Receiver receiver = expr.getScope().get().accept(this, context);
                context = context.copyChangeToParsingMemberOfReceiver(receiver);
                expr = expr.removeScope();
            }

            String methodName = expr.getNameAsString();

            // parse arguments list
            List<Receiver> arguments = new ArrayList<>();

            for (Expression argument : expr.getArguments()) {
                arguments.add(argument.accept(this, context.copyAndUseOuterReceiver()));
            }

            // get types for arguments
            List<TypeMirror> argumentTypes = new ArrayList<>();
            for (Receiver p : arguments) {
                argumentTypes.add(p.getType());
            }
            ExecutableElement methodElement;
            try {
                Element element = null;

                // try to find the correct method
                TypeMirror receiverType = context.receiver.getType();

                if (receiverType.getKind() == TypeKind.ARRAY) {
                    element = resolver.findMethod(methodName, receiverType, path, argumentTypes);
                }

                // Search for method in each enclosing class.
                while (receiverType.getKind() == TypeKind.DECLARED) {
                    element = resolver.findMethod(methodName, receiverType, path, argumentTypes);
                    if (element.getKind() == ElementKind.METHOD) {
                        break;
                    }
                    receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
                }

                if (element == null) {
                    throw constructParserException(s, "element==null");
                }
                if (element.getKind() != ElementKind.METHOD) {
                    throw constructParserException(s, "element.getKind()==" + element.getKind());
                }

                methodElement = (ExecutableElement) element;

                // Add valueOf around any arguments that require boxing.
                for (int i = 0; i < arguments.size(); i++) {
                    VariableElement parameter = methodElement.getParameters().get(i);
                    TypeMirror parameterType = parameter.asType();
                    Receiver argument = arguments.get(i);
                    TypeMirror argumentType = argument.getType();
                    // boxing necessary
                    if (TypesUtils.isBoxedPrimitive(parameterType)
                            && TypesUtils.isPrimitive(argumentType)) {
                        MethodSymbol valueOfMethod =
                                TreeBuilder.getValueOfMethod(env, parameterType);
                        List<Receiver> p = new ArrayList<>();
                        p.add(argument);
                        Receiver boxedParam =
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
                    throw new Error("no detail message in " + t.getClass(), t);
                }
                throw new ParseRuntimeException(constructParserException(s, t.getMessage()));
            }

            // TODO: reinstate this test, but issue a warning that the user
            // can override, rather than halting parsing which the user cannot override.
            /*if (!PurityUtils.isDeterministic(context.checkerContext.getAnnotationProvider(),
                    methodElement)) {
                throw new FlowExpressionParseException(new DiagMessage(ERROR,
                        "flowexpr.method.not.deterministic",
                        methodElement.getSimpleName()));
            }*/
            if (ElementUtils.isStatic(methodElement)) {
                Element classElem = methodElement.getEnclosingElement();
                Receiver staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
                return new MethodCall(
                        ElementUtils.getType(methodElement),
                        methodElement,
                        staticClassReceiver,
                        arguments);
            } else {
                if (context.receiver instanceof ClassName) {
                    throw new ParseRuntimeException(
                            constructParserException(
                                    s,
                                    "a non-static method call cannot have a class name as a receiver"));
                }
                TypeMirror methodType =
                        TypesUtils.substituteMethodReturnType(
                                methodElement, context.receiver.getType(), env);
                return new MethodCall(methodType, methodElement, context.receiver, arguments);
            }
        }

        /**
         * @param expr a field access, a fully qualified class name, or class name qualified with
         *     another class name (e.g. {@code OuterClass.InnerClass})
         */
        @Override
        public Receiver visit(FieldAccessExpr expr, FlowExpressionContext context) {
            Resolver resolver = new Resolver(env);

            Symbol.PackageSymbol packageSymbol =
                    resolver.findPackage(expr.getScope().toString(), path);
            if (packageSymbol != null) {
                ClassSymbol classSymbol =
                        resolver.findClassInPackage(expr.getNameAsString(), packageSymbol, path);
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

            Receiver receiver = expr.getScope().accept(this, context);

            // Parse the rest, with a new receiver.
            FlowExpressionContext newContext =
                    context.copyChangeToParsingMemberOfReceiver(receiver);
            return visit(expr.getNameAsExpression(), newContext);
        }

        /**
         * Returns a NameExpr to be handled by NameExpr visitor or a FieldAccessExpr to be handled
         * by FieldAccess visitor.
         *
         * @param expr a Class Literal
         * @return a NameExpr to be handled by NameExpr visitor or a FieldAccessExpr to be handled
         *     by FieldAccess visitor
         */
        @Override
        public Receiver visit(ClassExpr expr, FlowExpressionContext context) {
            TypeMirror result = convertTypeToTypeMirror(expr.getType(), context);
            if (result == null) {
                throw new ParseRuntimeException(
                        constructParserException(
                                expr.toString(), "is an unparsable class literal"));
            }
            return new ClassName(result);
        }

        /** @param expr an array creation expression, with dimensions and/or initializers. */
        @Override
        public Receiver visit(ArrayCreationExpr expr, FlowExpressionContext context) {
            List<Receiver> dimensions = new ArrayList<>();
            for (ArrayCreationLevel dimension : expr.getLevels()) {
                if (dimension.getDimension().isPresent()) {
                    dimensions.add(dimension.getDimension().get().accept(this, context));
                } else {
                    dimensions.add(null);
                }
            }

            List<Receiver> initializers = new ArrayList<>();
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
         * Converts the JavaParser type to a TypeMirror.
         *
         * <p>Might return null if convert the kind of type is not handled.
         *
         * @param type JavaParser type
         * @param context FlowExpressionContext
         * @return TypeMirror corresponding to {@code type} or null if {@code type} isn't handled
         */
        private @Nullable TypeMirror convertTypeToTypeMirror(
                Type type, FlowExpressionContext context) {
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
         * Returns the receiver of the passed String name.
         *
         * @param s a String representing an identifier (name expression, no dots in it)
         * @return the receiver of the passed String name
         */
        private static Receiver getReceiverField(
                String s,
                FlowExpressionContext context,
                boolean originalReceiver,
                VariableElement fieldElem) {
            TypeMirror receiverType = context.receiver.getType();

            TypeMirror fieldType = ElementUtils.getType(fieldElem);
            if (ElementUtils.isStatic(fieldElem)) {
                Element classElem = fieldElem.getEnclosingElement();
                Receiver staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
                return new FieldAccess(staticClassReceiver, fieldType, fieldElem);
            }
            Receiver locationOfField;
            if (originalReceiver) {
                locationOfField = context.receiver;
            } else {
                locationOfField =
                        FlowExpressions.internalReprOf(
                                context.checkerContext.getAnnotationProvider(),
                                new ImplicitThisLiteralNode(receiverType));
            }
            if (locationOfField instanceof ClassName) {
                throw new ParseRuntimeException(
                        constructParserException(
                                s, "a non-static field cannot have a class name as a receiver."));
            }
            return new FieldAccess(locationOfField, fieldType, fieldElem);
        }

        /**
         * Returns the receiver of the parameter passed.
         *
         * @param s a String that starts with PARAMETER_REPLACEMENT
         * @return the receiver of the parameter passed
         */
        private static Receiver getParameterReceiver(String s, FlowExpressionContext context) {
            if (context.arguments == null) {
                throw new ParseRuntimeException(constructParserException(s, "no parameter found"));
            }
            int idx = Integer.parseInt(s.substring(PARAMETER_REPLACEMENT_LENGTH));

            if (idx == 0) {
                throw new ParseRuntimeException(
                        constructParserException(
                                s,
                                "one should use \"this\" for the receiver or \"#1\" for the first formal parameter"));
            }
            if (idx > context.arguments.size()) {
                throw new ParseRuntimeException(
                        new FlowExpressionParseException(
                                "flowexpr.parse.index.too.big", Integer.toString(idx)));
            }
            return context.arguments.get(idx - 1);
        }
    }

    /**
     * Returns a list of 1-based indices of all formal parameters that occur in {@code s}. Each
     * formal parameter occurs in s as a string like "#1" or "#4". This routine does not do proper
     * parsing; for instance, if "#2" appears within a string in s, then 2 would still be in the
     * result list.
     *
     * @return a list of 1-based indices of all formal parameters that occur in {@code s}.
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
     * Context used to parse a flow expression. When parsing flow expression E in annotation
     * {@code @A(E)}, the context is the program element that is annotated by {@code @A(E)}.
     */
    public static class FlowExpressionContext {
        public final Receiver receiver;
        /**
         * In a context for a method declaration or lambda, the formals. In a context for a method
         * invocation, the actuals. In a context for a class declaration, an empty list.
         */
        public final List<Receiver> arguments;

        public final Receiver outerReceiver;
        public final BaseContext checkerContext;
        /**
         * Whether or not the FlowExpressionParser is parsing the "member" part of a member select.
         */
        public final boolean parsingMember;
        /** Whether the TreePath should be used to find identifiers. Defaults to true. */
        public final boolean useLocalScope;

        /**
         * Creates context for parsing a flow expression.
         *
         * @param receiver used to replace "this" in a flow expression and used to resolve
         *     identifiers in the flow expression with an implicit "this"
         * @param arguments used to replace parameter references, e.g. #1, in flow expressions, null
         *     if no arguments
         * @param checkerContext used to create {@link
         *     org.checkerframework.dataflow.analysis.FlowExpressions.Receiver}s
         */
        public FlowExpressionContext(
                Receiver receiver, List<Receiver> arguments, BaseContext checkerContext) {
            this(receiver, receiver, arguments, checkerContext);
        }

        private FlowExpressionContext(
                Receiver receiver,
                Receiver outerReceiver,
                List<Receiver> arguments,
                BaseContext checkerContext) {
            this(receiver, outerReceiver, arguments, checkerContext, false, true);
        }

        private FlowExpressionContext(
                Receiver receiver,
                Receiver outerReceiver,
                List<Receiver> arguments,
                BaseContext checkerContext,
                boolean parsingMember,
                boolean useLocalScope) {
            assert checkerContext != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.outerReceiver = outerReceiver;
            this.checkerContext = checkerContext;
            this.parsingMember = parsingMember;
            this.useLocalScope = useLocalScope;
        }

        /**
         * Creates a {@link FlowExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used translate parameter numbers in a flow expression to formal
         *     parameters of the method
         * @param enclosingTree used to look up fields and as type of "this" in flow expressions
         * @param checkerContext use to build FlowExpressions.Receiver
         * @return context created of {@code methodDeclaration}
         */
        public static FlowExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, Tree enclosingTree, BaseContext checkerContext) {
            return buildContextForMethodDeclaration(
                    methodDeclaration, TreeUtils.typeOf(enclosingTree), checkerContext);
        }

        /**
         * Creates a {@link FlowExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used translate parameter numbers in a flow expression to formal
         *     parameters of the method
         * @param enclosingType used to look up fields and as type of "this" in flow expressions
         * @param checkerContext use to build FlowExpressions.Receiver
         * @return context created of {@code methodDeclaration}
         */
        public static FlowExpressionContext buildContextForMethodDeclaration(
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
                receiver = new ImplicitThisLiteralNode(enclosingType);
            }
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            for (VariableTree arg : methodDeclaration.getParameters()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(),
                                new LocalVariableNode(arg, receiver)));
            }
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        public static FlowExpressionContext buildContextForLambda(
                LambdaExpressionTree lambdaTree, TreePath path, BaseContext checkerContext) {
            TypeMirror enclosingType = TreeUtils.typeOf(TreeUtils.enclosingClass(path));
            Node receiver = new ImplicitThisLiteralNode(enclosingType);
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            for (VariableTree arg : lambdaTree.getParameters()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(),
                                new LocalVariableNode(arg, receiver)));
            }
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * Creates a {@link FlowExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used translate parameter numbers in a flow expression to formal
         *     parameters of the method
         * @param currentPath to find the enclosing class, which is used to look up fields and as
         *     type of "this" in flow expressions
         * @param checkerContext use to build FlowExpressions.Receiver
         * @return context created of {@code methodDeclaration}
         */
        public static FlowExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, TreePath currentPath, BaseContext checkerContext) {
            Tree classTree = TreeUtils.enclosingClass(currentPath);
            return buildContextForMethodDeclaration(methodDeclaration, classTree, checkerContext);
        }

        /**
         * Returns a {@link FlowExpressionContext} for the class {@code classTree} as seen at the
         * class declaration.
         *
         * @return a {@link FlowExpressionContext} for the class {@code classTree} as seen at the
         *     class declaration
         */
        public static FlowExpressionContext buildContextForClassDeclaration(
                ClassTree classTree, BaseContext checkerContext) {
            Node receiver = new ImplicitThisLiteralNode(TreeUtils.typeOf(classTree));

            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * Returns a {@link FlowExpressionContext} for the method {@code methodInvocation}
         * (represented as a {@link Node} as seen at the method use (i.e., at a method call site).
         *
         * @return a {@link FlowExpressionContext} for the method {@code methodInvocation}
         */
        public static FlowExpressionContext buildContextForMethodUse(
                MethodInvocationNode methodInvocation, BaseContext checkerContext) {
            Node receiver = methodInvocation.getTarget().getReceiver();
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(
                            checkerContext.getAnnotationProvider(), receiver);
            List<Receiver> internalArguments = new ArrayList<>();
            for (Node arg : methodInvocation.getArguments()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), arg));
            }
            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * Returns a {@link FlowExpressionContext} for the method {@code methodInvocation}
         * (represented as a {@link MethodInvocationTree} as seen at the method use (i.e., at a
         * method call site).
         *
         * @return a {@link FlowExpressionContext} for the method {@code methodInvocation}
         */
        public static FlowExpressionContext buildContextForMethodUse(
                MethodInvocationTree methodInvocation, BaseContext checkerContext) {
            ExpressionTree receiverTree = TreeUtils.getReceiverTree(methodInvocation);
            FlowExpressions.Receiver receiver;
            if (receiverTree == null) {
                receiver =
                        FlowExpressions.internalReprOfImplicitReceiver(
                                TreeUtils.elementFromUse(methodInvocation));
            } else {
                receiver =
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), receiverTree);
            }

            List<? extends ExpressionTree> args = methodInvocation.getArguments();
            List<FlowExpressions.Receiver> argReceivers = new ArrayList<>(args.size());
            for (ExpressionTree argTree : args) {
                argReceivers.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), argTree));
            }

            return new FlowExpressionContext(receiver, argReceivers, checkerContext);
        }

        /**
         * Returns a {@link FlowExpressionContext} for the constructor {@code n} (represented as a
         * {@link Node} as seen at the method use (i.e., at a method call site).
         *
         * @return a {@link FlowExpressionContext} for the constructor {@code n} (represented as a
         *     {@link Node} as seen at the method use (i.e., at a method call site)
         */
        public static FlowExpressionContext buildContextForNewClassUse(
                ObjectCreationNode n, BaseContext checkerContext) {

            // This returns an FlowExpressions.Unknown with the type set to the class in which the
            // constructor is declared
            Receiver internalReceiver =
                    FlowExpressions.internalReprOf(checkerContext.getAnnotationProvider(), n);

            List<Receiver> internalArguments = new ArrayList<>();
            for (Node arg : n.getArguments()) {
                internalArguments.add(
                        FlowExpressions.internalReprOf(
                                checkerContext.getAnnotationProvider(), arg));
            }

            FlowExpressionContext flowExprContext =
                    new FlowExpressionContext(internalReceiver, internalArguments, checkerContext);
            return flowExprContext;
        }

        /**
         * Returns a copy of the context that differs in that it has a different receiver and
         * parsingMember is set to true. The outer receiver remains unchanged.
         */
        public FlowExpressionContext copyChangeToParsingMemberOfReceiver(Receiver receiver) {
            return new FlowExpressionContext(
                    receiver,
                    outerReceiver,
                    arguments,
                    checkerContext,
                    /*parsingMember=*/ true,
                    useLocalScope);
        }

        /**
         * Returns a copy of the context that differs in that it uses the outer receiver as main
         * receiver (and also retains it as the outer receiver), and parsingMember is set to false.
         */
        public FlowExpressionContext copyAndUseOuterReceiver() {
            return new FlowExpressionContext(
                    outerReceiver, // NOTE different than in this object
                    outerReceiver,
                    arguments,
                    checkerContext,
                    /*parsingMember=*/ false,
                    useLocalScope);
        }

        /**
         * Returns a copy of the context that differs in that useLocalScope is set to the given
         * value.
         */
        public FlowExpressionContext copyAndSetUseLocalScope(boolean useLocalScope) {
            return new FlowExpressionContext(
                    receiver,
                    outerReceiver,
                    arguments,
                    checkerContext,
                    parsingMember,
                    useLocalScope);
        }

        /**
         * Format this object verbosely, with each line indented by 4 spaces but without a trailing
         * newline.
         *
         * @return a verbose string representation of this
         */
        public String toStringDebug() {
            return toStringDebug(4);
        }

        /**
         * Format this object verbosely, with each line indented by the given number of spaces but
         * without a trailing newline.
         *
         * @param indent the number of spaces to indent the string representation of this
         * @return a verbose string representation of this
         */
        public String toStringDebug(int indent) {
            String indentString = String.join("", Collections.nCopies(indent, " "));
            StringJoiner sj = new StringJoiner(indentString, indentString, "");
            sj.add(String.format("receiver=%s%n", receiver.toStringDebug()));
            sj.add(String.format("arguments=%s%n", arguments));
            sj.add(String.format("outerReceiver=%s%n", outerReceiver.toStringDebug()));
            sj.add(String.format("checkerContext=%s%n", "..."));
            // sj.add(String.format("checkerContext=%s%n", checkerContext));
            sj.add(String.format("parsingMember=%s%n", parsingMember));
            sj.add(String.format("useLocalScope=%s", useLocalScope));
            return sj.toString();
        }
    }

    /**
     * Returns the type of the inner most enclosing class.Type.noType is returned if no enclosing
     * class is found. This is in contrast to {@link DeclaredType#getEnclosingType()} which returns
     * the type of the inner most instance. If the inner most enclosing class is static this method
     * will return the type of that class where as {@link DeclaredType#getEnclosingType()} will
     * return the type of the inner most enclosing class that is not static.
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

    public static Receiver internalReprOfVariable(AnnotatedTypeFactory provider, VariableTree tree)
            throws FlowExpressionParseException {
        Element elt = TreeUtils.elementFromDeclaration(tree);

        if (elt.getKind() == ElementKind.LOCAL_VARIABLE
                || elt.getKind() == ElementKind.RESOURCE_VARIABLE
                || elt.getKind() == ElementKind.EXCEPTION_PARAMETER
                || elt.getKind() == ElementKind.PARAMETER) {
            return new LocalVariable(elt);
        }
        Receiver receiverF = FlowExpressions.internalReprOfImplicitReceiver(elt);
        FlowExpressionContext context =
                new FlowExpressionContext(receiverF, null, provider.getContext());
        return parse(tree.getName().toString(), context, provider.getPath(tree), false);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Exceptions
    ///

    /**
     * An exception that indicates a parse error. Call {@link #getDiagMessage} to obtain a {@link
     * DiagMessage} that can be used for error reporting.
     */
    public static class FlowExpressionParseException extends Exception {
        private static final long serialVersionUID = 2L;
        private @CompilerMessageKey String errorKey;
        public final Object[] args;

        public FlowExpressionParseException(@CompilerMessageKey String errorKey, Object... args) {
            this(null, errorKey, args);
        }

        public FlowExpressionParseException(
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
     * Returns a {@link FlowExpressionParseException} for the expression {@code expr} with
     * explanation {@code explanation}.
     */
    private static FlowExpressionParseException constructParserException(
            String expr, String explanation) {
        if (expr == null) {
            throw new Error("Must have an expression.");
        }
        if (explanation == null) {
            throw new Error("Must have an explanation.");
        }
        return new FlowExpressionParseException(
                (Throwable) null,
                "flowexpr.parse.error",
                "Invalid '" + expr + "' because " + explanation);
    }

    /**
     * The Runtime equivalent of {@link FlowExpressionParseException}. This class is needed to wrap
     * this exception into an unchecked exception.
     */
    private static class ParseRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 2L;
        private final FlowExpressionParseException exception;

        private ParseRuntimeException(FlowExpressionParseException exception) {
            this.exception = exception;
        }

        private FlowExpressionParseException getCheckedException() {
            return exception;
        }
    }
}
