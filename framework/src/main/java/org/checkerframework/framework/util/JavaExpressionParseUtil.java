package org.checkerframework.framework.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
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
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
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
import javax.lang.model.element.PackageElement;
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
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ArrayCreation;
import org.checkerframework.dataflow.expression.BinaryOperation;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.UnaryOperation;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Resolver;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

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
     * Parsable replacement for parameter references. It is parsable because it is a Java
     * identifier.
     */
    private static final String PARAMETER_REPLACEMENT = "_param_";

    /** The length of {@link #PARAMETER_REPLACEMENT}. */
    private static final int PARAMETER_REPLACEMENT_LENGTH = PARAMETER_REPLACEMENT.length();

    /**
     * Parse a string and viewpoint-adapt it to the given {@code context}. Return its representation
     * as a {@link JavaExpression}, or throw a {@link JavaExpressionParseException}.
     *
     * @param expression a Java expression to parse
     * @param context information about any receiver and arguments; also has a reference to the
     *     checker
     * @return the JavaExpression for the given string
     * @throws JavaExpressionParseException if the string cannot be parsed
     */
    public static JavaExpression parse(String expression, JavaExpressionContext context)
            throws JavaExpressionParseException {
        return parse(expression, context, null);
    }
    /**
     * Parse a string with respect to {@code localPath} and viewpoint-adapt it to the given {@code
     * context}. Return its representation as a {@link JavaExpression}, or throw a {@link
     * JavaExpressionParseException}.
     *
     * <p>If {@code localPath} is non-null, then identifiers are parsed as if the expression was
     * written at the location of {@code localPath}. This means identifiers will be parsed to local
     * variables in scope at {@code localPath} when possible. If {@code localPath} is null, then no
     * identifier can be parsed to a local variable. In either case, the parameter syntax, e.g. #1,
     * is always parsed to the arguments in {@code context}. This is because a parameter of a lambda
     * can refer both to local variables in scope at its declaration and to a parameter of the
     * lambda.
     *
     * @param expression a Java expression to parse
     * @param context information about any receiver and arguments; also has a reference to the
     *     checker
     * @param localPath if non-null, the expression is parsed as if it were written at this location
     * @return the JavaExpression for the given string
     * @throws JavaExpressionParseException if the string cannot be parsed
     */
    public static JavaExpression parse(
            String expression, JavaExpressionContext context, @Nullable TreePath localPath)
            throws JavaExpressionParseException {
        // The underlying javac API used to convert from Strings to Elements requires a tree path
        // even when the information could be deduced from elements alone.  So use the path to the
        // current CompilationUnit.
        TreePath pathToCompilationUnit = context.checker.getPathToCompilationUnit();
        Expression expr;
        try {
            expr = StaticJavaParser.parseExpression(replaceParameterSyntax(expression));
        } catch (ParseProblemException e) {
            throw constructJavaExpressionParseError(expression, "is an invalid expression");
        }

        JavaExpression result;
        try {
            ProcessingEnvironment env = context.checker.getProcessingEnvironment();
            result =
                    expr.accept(
                            new ExpressionToJavaExpressionVisitor(
                                    pathToCompilationUnit, localPath, env),
                            context);
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
            throw constructJavaExpressionParseError(
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
         * The underlying javac API used to convert from Strings to Elements requires a tree path
         * even when the information could be deduced from elements alone. So use the path to the
         * current CompilationUnit.
         */
        private final TreePath pathToCompilationUnit;
        /** If non-null, the expression is parsed as if it were written at this location. */
        private final @Nullable TreePath localVarPath;
        /** The processing environment. */
        private final ProcessingEnvironment env;
        /** The resolver. Computed from the environment, but lazily initialized. */
        private @MonotonicNonNull Resolver resolver = null;
        /** The type utilities. */
        private final Types types;

        /** The java.lang.String type. */
        TypeMirror stringTypeMirror;

        /**
         * Create a new ExpressionToJavaExpressionVisitor.
         *
         * @param pathToCompilationUnit required to use the underlying Javac API
         * @param localVarPath if non-null, the expression is parsed as if it were written at this
         *     location
         * @param env the processing environment
         */
        ExpressionToJavaExpressionVisitor(
                TreePath pathToCompilationUnit,
                @Nullable TreePath localVarPath,
                ProcessingEnvironment env) {
            this.pathToCompilationUnit = pathToCompilationUnit;
            this.localVarPath = localVarPath;
            this.env = env;
            this.types = env.getTypeUtils();
            this.stringTypeMirror =
                    env.getElementUtils().getTypeElement("java.lang.String").asType();
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
            throw new ParseRuntimeException(
                    constructJavaExpressionParseError(n.toString(), message));
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
            return new ValueLiteral(stringTypeMirror, expr.asString());
        }

        @Override
        public JavaExpression visit(BooleanLiteralExpr expr, JavaExpressionContext context) {
            return new ValueLiteral(types.getPrimitiveType(TypeKind.BOOLEAN), expr.getValue());
        }

        @Override
        public JavaExpression visit(ThisExpr n, JavaExpressionContext context) {
            if (context.receiver == null || context.receiver instanceof ClassName) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError("this", "this isn't allowed here"));
            }
            // "this" is the receiver of the context
            return context.receiver;
        }

        @Override
        public JavaExpression visit(SuperExpr n, JavaExpressionContext context) {
            // super literal
            TypeMirror superclass = TypesUtils.getSuperclass(context.receiver.getType(), types);
            if (superclass == null) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError("super", "super class not found"));
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
                        constructJavaExpressionParseError(
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
            if (s.startsWith(PARAMETER_REPLACEMENT)) {
                // A parameter is a local variable, but it can be referenced outside of local scope
                // (at the method scope) using the special #NN syntax.
                return getParameterJavaExpression(s, context);
            }

            // Local variable or parameter.
            if (localVarPath != null) {
                // Attempt to match a local variable within the scope of the
                // given path before attempting to match a field.
                VariableElement varElem = resolver.findLocalVariableOrParameter(s, localVarPath);
                if (varElem != null) {
                    return new LocalVariable(varElem);
                }
            }

            // Field access
            FieldAccess fieldAccess = getIdentifierAsField(context.receiver, s);
            if (fieldAccess != null) {
                return fieldAccess;
            }

            if (localVarPath != null) {
                Element classElem = resolver.findClass(s, localVarPath);
                TypeMirror classType = ElementUtils.getType(classElem);
                if (classType != null) {
                    return new ClassName(classType);
                }
            }

            ClassName classType =
                    getIdentifierAsUnqualifiedClassName(context.receiver.getType(), s);
            if (classType != null) {
                return classType;
            }

            // Err if a formal parameter name is used, instead of the "#2" syntax.
            if (context.arguments != null) {
                for (int i = 0; i < context.arguments.size(); i++) {
                    if (context.arguments.get(i) instanceof LocalVariable) {
                        Element varElt = ((LocalVariable) context.arguments.get(i)).getElement();
                        if (varElt.getKind() == ElementKind.PARAMETER
                                && varElt.getSimpleName().contentEquals(s)) {
                            throw new ParseRuntimeException(
                                    constructJavaExpressionParseError(
                                            s,
                                            String.format(
                                                    DependentTypesError.FORMAL_PARAM_NAME_STRING,
                                                    i + 1,
                                                    s)));
                        }
                    }
                }
            }

            throw new ParseRuntimeException(
                    constructJavaExpressionParseError(s, "identifier not found"));
        }

        /**
         * If {@code identifier} is the simple class name of any inner class of {@code type}, return
         * the {@link ClassName} for the inner class. If not, return null.
         *
         * @param type type to search for {@code identifier}
         * @param identifier possible class name
         * @return the {@code ClassName} for {@code identifier} or null if it is not a class name
         */
        protected @Nullable ClassName getIdentifierAsInnerClassName(
                TypeMirror type, String identifier) {
            if (type.getKind() != TypeKind.DECLARED) {
                return null;
            }

            Element outerClass = ((DeclaredType) type).asElement();
            for (Element memberElement : outerClass.getEnclosedElements()) {
                if (!(memberElement.getKind().isClass() || memberElement.getKind().isInterface())) {
                    continue;
                }
                if (memberElement.getSimpleName().contentEquals(identifier)) {
                    return new ClassName(ElementUtils.getType(memberElement));
                }
            }
            return null;
        }

        /**
         * If {@code identifier} is a class name with that can be referenced using only its simple
         * name within {@code type}, return it. If not, return null.
         *
         * <p>{@code identifier} may be
         *
         * <ol>
         *   <li>the simple name of {@code type}.
         *   <li>the simple name of a class declared in {@code type} or in an enclosing type of
         *       {@code type}.
         *   <li>the simple name of a class in the java.lang package.
         *   <li>the simple name of a class in the unnamed package.
         * </ol>
         *
         * @param type the type in which to search for {@code identifier}
         * @param identifier possible class name
         * @return the {@code ClassName} for {@code identifier} or null if it is not a class name
         */
        protected @Nullable ClassName getIdentifierAsUnqualifiedClassName(
                TypeMirror type, String identifier) {
            // Is identifier an inner class of this or of any enclosing class of this?
            TypeMirror searchType = type;
            while (searchType.getKind() == TypeKind.DECLARED) {
                // Is identifier the simple name of this?
                if (((DeclaredType) searchType)
                        .asElement()
                        .getSimpleName()
                        .contentEquals(identifier)) {
                    return new ClassName(searchType);
                }
                ClassName className = getIdentifierAsInnerClassName(searchType, identifier);
                if (className != null) {
                    return className;
                }
                searchType = getTypeOfEnclosingClass((DeclaredType) searchType);
            }

            if (type.getKind() == TypeKind.DECLARED) {
                // Is identifier in the same package as this?
                PackageSymbol packageSymbol =
                        (PackageSymbol)
                                ElementUtils.enclosingPackage(((DeclaredType) type).asElement());
                ClassSymbol classSymbol =
                        resolver.findClassInPackage(
                                identifier, packageSymbol, pathToCompilationUnit);
                if (classSymbol != null) {
                    return new ClassName(classSymbol.asType());
                }
            }
            // Is identifier a simple name for a class in java.lang?
            Symbol.PackageSymbol packageSymbol =
                    resolver.findPackage("java.lang", pathToCompilationUnit);
            if (packageSymbol != null) {
                ClassSymbol classSymbol =
                        resolver.findClassInPackage(
                                identifier, packageSymbol, pathToCompilationUnit);
                if (classSymbol != null) {
                    return new ClassName(classSymbol.asType());
                }
            }

            // Is identifier a class in the unnamed package?
            Element classElem = resolver.findClass(identifier, pathToCompilationUnit);
            if (classElem != null) {
                PackageElement pkg = ElementUtils.enclosingPackage(classElem);
                if (pkg != null && pkg.isUnnamed()) {
                    TypeMirror classType = ElementUtils.getType(classElem);
                    if (classType != null) {
                        return new ClassName(classType);
                    }
                }
            }

            return null;
        }

        /**
         * Return the {@link FieldAccess} expression for the field with name {@code identifier}
         * accessed via {@code receiverExpr}. If no such field exists, then {@code null} is
         * returned.
         *
         * @param receiverExpr the receiver of the field access; the expression used to access the
         *     field
         * @param identifier possibly a field name
         * @return a field access, or null if {@code identifier} is not a field that can be accessed
         *     via {@code receiverExpr}
         */
        protected @Nullable FieldAccess getIdentifierAsField(
                JavaExpression receiverExpr, String identifier) {
            // Find the field element.
            TypeMirror enclosingTypeOfField = receiverExpr.getType();
            VariableElement fieldElem;
            if (identifier.equals("length") && enclosingTypeOfField.getKind() == TypeKind.ARRAY) {
                fieldElem =
                        resolver.findField(identifier, enclosingTypeOfField, pathToCompilationUnit);
                if (fieldElem == null) {
                    throw new BugInCF("length field not found for type: %s", enclosingTypeOfField);
                }
            } else {
                fieldElem = null;
                // Search for field in each enclosing class.
                while (enclosingTypeOfField.getKind() == TypeKind.DECLARED) {
                    fieldElem =
                            resolver.findField(
                                    identifier, enclosingTypeOfField, pathToCompilationUnit);
                    if (fieldElem != null) {
                        break;
                    }
                    enclosingTypeOfField =
                            getTypeOfEnclosingClass((DeclaredType) enclosingTypeOfField);
                }
                if (fieldElem == null) {
                    // field not found.
                    return null;
                }
            }

            // Construct a FieldAccess expression.
            if (ElementUtils.isStatic(fieldElem)) {
                Element classElem = fieldElem.getEnclosingElement();
                JavaExpression staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
                return new FieldAccess(staticClassReceiver, fieldElem);
            }
            if (receiverExpr instanceof ClassName) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
                                fieldElem.getSimpleName().toString(),
                                "a non-static field cannot have a class name as a receiver."));
            }

            @SuppressWarnings("interning:not.interned") // Checking for exact object
            boolean fieldDeclaredInReceiverType = enclosingTypeOfField == receiverExpr.getType();
            // fieldElem is an instance field
            if (fieldDeclaredInReceiverType) {
                // It's an instance field declared in the type (or supertype) of receiverExpr.
                TypeMirror fieldType = ElementUtils.getType(fieldElem);
                return new FieldAccess(receiverExpr, fieldType, fieldElem);
            }
            if (!(receiverExpr instanceof ThisReference)) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
                                identifier,
                                "field declared in an outer type cannot be accessed from an inner type"));
            }
            TypeElement receiverTypeElement = TypesUtils.getTypeElement(receiverExpr.getType());
            if (receiverTypeElement == null || ElementUtils.isStatic(receiverTypeElement)) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
                                identifier,
                                "a non-static field declared in an outer type cannot be referenced from a member type"));
            }
            // It's an instance field declared in an enclosing type of receiverExpr, and
            // enclosingTypeOfField != receiverExpr.getType().
            JavaExpression locationOfField = new ThisReference(enclosingTypeOfField);
            return new FieldAccess(locationOfField, fieldElem);
        }

        @Override
        public JavaExpression visit(MethodCallExpr expr, JavaExpressionContext context) {
            setResolverField();

            JavaExpression receiverExpr;
            if (expr.getScope().isPresent()) {
                receiverExpr = expr.getScope().get().accept(this, context);
                expr = expr.removeScope();
            } else {
                receiverExpr = context.receiver;
            }

            String methodName = expr.getNameAsString();

            // parse argument list
            List<JavaExpression> arguments;
            if (expr.getArguments().isEmpty()) {
                arguments = Collections.emptyList();
            } else {
                JavaExpressionContext argContext = context;
                arguments =
                        SystemUtil.mapList(
                                (Expression argument) -> argument.accept(this, argContext),
                                expr.getArguments());
            }

            // Find the method element.
            ExecutableElement methodElement;
            try {
                methodElement =
                        getMethodElement(
                                methodName,
                                receiverExpr.getType(),
                                pathToCompilationUnit,
                                arguments,
                                resolver);
            } catch (JavaExpressionParseException e) {
                throw new ParseRuntimeException(e);
            }

            // Box any arguments that require it.
            for (int i = 0; i < arguments.size(); i++) {
                VariableElement parameter = methodElement.getParameters().get(i);
                TypeMirror parameterType = parameter.asType();
                JavaExpression argument = arguments.get(i);
                TypeMirror argumentType = argument.getType();
                // is boxing necessary?
                if (TypesUtils.isBoxedPrimitive(parameterType)
                        && TypesUtils.isPrimitive(argumentType)) {
                    // boxing is necessary
                    MethodSymbol valueOfMethod = TreeBuilder.getValueOfMethod(env, parameterType);
                    JavaExpression boxedParam =
                            new MethodCall(
                                    parameterType,
                                    valueOfMethod,
                                    new ClassName(parameterType),
                                    Collections.singletonList(argument));
                    arguments.set(i, boxedParam);
                }
            }

            // Build the MethodCall expression object.
            if (ElementUtils.isStatic(methodElement)) {
                Element classElem = methodElement.getEnclosingElement();
                JavaExpression staticClassReceiver = new ClassName(ElementUtils.getType(classElem));
                return new MethodCall(
                        ElementUtils.getType(methodElement),
                        methodElement,
                        staticClassReceiver,
                        arguments);
            } else {
                if (receiverExpr instanceof ClassName) {
                    throw new ParseRuntimeException(
                            constructJavaExpressionParseError(
                                    expr.toString(),
                                    "a non-static method call cannot have a class name as a receiver"));
                }
                TypeMirror methodType =
                        TypesUtils.substituteMethodReturnType(
                                methodElement, receiverExpr.getType(), env);
                return new MethodCall(methodType, methodElement, receiverExpr, arguments);
            }
        }

        /**
         * Returns the ExecutableElement for a method, or throws an exception.
         *
         * <p>(This method takes into account autoboxing.)
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

            List<TypeMirror> argumentTypes = SystemUtil.mapList(JavaExpression::getType, arguments);

            if (receiverType.getKind() == TypeKind.ARRAY) {
                ExecutableElement element =
                        resolver.findMethod(methodName, receiverType, path, argumentTypes);
                if (element == null) {
                    throw constructJavaExpressionParseError(methodName, "no such method");
                }
                return element;
            }

            // Search for method in each enclosing class.
            while (receiverType.getKind() == TypeKind.DECLARED) {
                ExecutableElement element =
                        resolver.findMethod(methodName, receiverType, path, argumentTypes);
                if (element != null) {
                    return element;
                }
                receiverType = getTypeOfEnclosingClass((DeclaredType) receiverType);
            }

            // Method not found.
            throw constructJavaExpressionParseError(methodName, "no such method");
        }

        // expr is a field access, a fully qualified class name, or a class name qualified with
        // another class name (e.g. {@code OuterClass.InnerClass})
        @Override
        public JavaExpression visit(FieldAccessExpr expr, JavaExpressionContext context) {
            setResolverField();

            // Check for fully qualified class name.
            Symbol.PackageSymbol packageSymbol =
                    resolver.findPackage(expr.getScope().toString(), pathToCompilationUnit);
            if (packageSymbol != null) {
                ClassSymbol classSymbol =
                        resolver.findClassInPackage(
                                expr.getNameAsString(), packageSymbol, pathToCompilationUnit);
                if (classSymbol != null) {
                    return new ClassName(classSymbol.asType());
                }
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
                                expr.toString(),
                                "could not find class "
                                        + expr.getNameAsString()
                                        + " inside "
                                        + expr.getScope().toString()));
            }

            // Check for field access expression.
            String identifier = expr.getName().getIdentifier();
            JavaExpression receiver = expr.getScope().accept(this, context);
            FieldAccess fieldAccess = getIdentifierAsField(receiver, identifier);
            if (fieldAccess != null) {
                return fieldAccess;
            }

            // Check for inner class.
            ClassName classType = getIdentifierAsInnerClassName(receiver.getType(), identifier);
            if (classType != null) {
                return classType;
            }
            throw new ParseRuntimeException(
                    constructJavaExpressionParseError(
                            identifier,
                            String.format(
                                    "field or class %s not found in %s", identifier, receiver)));
        }

        // expr is a Class literal
        @Override
        public JavaExpression visit(ClassExpr expr, JavaExpressionContext context) {
            TypeMirror result = convertTypeToTypeMirror(expr.getType(), context);
            if (result == null) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
                                expr.toString(), "is an unparsable class literal"));
            }
            return new ClassName(result);
        }

        @Override
        public JavaExpression visit(ArrayCreationExpr expr, JavaExpressionContext context) {
            List<JavaExpression> dimensions =
                    SystemUtil.mapList(
                            (ArrayCreationLevel dimension) ->
                                    dimension.getDimension().isPresent()
                                            ? dimension.getDimension().get().accept(this, context)
                                            : null,
                            expr.getLevels());

            List<JavaExpression> initializers;
            if (expr.getInitializer().isPresent()) {
                initializers =
                        SystemUtil.mapList(
                                (Expression initializer) -> initializer.accept(this, context),
                                expr.getInitializer().get().getValues());
            } else {
                initializers = Collections.emptyList();
            }
            TypeMirror arrayType = convertTypeToTypeMirror(expr.getElementType(), context);
            if (arrayType == null) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
                                expr.getElementType().asString(), "type not parsable"));
            }
            for (int i = 0; i < dimensions.size(); i++) {
                arrayType = TypesUtils.createArrayType(arrayType, env.getTypeUtils());
            }
            return new ArrayCreation(arrayType, dimensions, initializers);
        }

        @Override
        public JavaExpression visit(UnaryExpr expr, JavaExpressionContext context) {
            Tree.Kind treeKind = javaParserUnaryOperatorToTreeKind(expr.getOperator());
            // This performs constant-folding for + and -; it could also do so for other operations.
            switch (treeKind) {
                case UNARY_PLUS:
                    return expr.getExpression().accept(this, context);
                case UNARY_MINUS:
                    JavaExpression negatedResult = expr.getExpression().accept(this, context);
                    if (negatedResult instanceof ValueLiteral) {
                        return ((ValueLiteral) negatedResult).negate();
                    }
                    return new UnaryOperation(negatedResult.getType(), treeKind, negatedResult);
                default:
                    JavaExpression operand = expr.getExpression().accept(this, context);
                    return new UnaryOperation(operand.getType(), treeKind, operand);
            }
        }

        /**
         * Convert a JavaParser unary operator to a TreeKind.
         *
         * @param op a JavaParser unary operator
         * @return a TreeKind for the unary operator
         */
        Tree.Kind javaParserUnaryOperatorToTreeKind(UnaryExpr.Operator op) {
            switch (op) {
                case BITWISE_COMPLEMENT:
                    return Tree.Kind.BITWISE_COMPLEMENT;
                case LOGICAL_COMPLEMENT:
                    return Tree.Kind.LOGICAL_COMPLEMENT;
                case MINUS:
                    return Tree.Kind.UNARY_MINUS;
                case PLUS:
                    return Tree.Kind.UNARY_PLUS;
                case POSTFIX_DECREMENT:
                    return Tree.Kind.POSTFIX_DECREMENT;
                case POSTFIX_INCREMENT:
                    return Tree.Kind.POSTFIX_INCREMENT;
                case PREFIX_DECREMENT:
                    return Tree.Kind.PREFIX_DECREMENT;
                case PREFIX_INCREMENT:
                    return Tree.Kind.PREFIX_INCREMENT;
                default:
                    throw new Error("unhandled " + op);
            }
        }

        @Override
        public JavaExpression visit(BinaryExpr expr, JavaExpressionContext context) {
            JavaExpression leftJe = expr.getLeft().accept(this, context);
            JavaExpression rightJe = expr.getRight().accept(this, context);
            TypeMirror leftType = leftJe.getType();
            TypeMirror rightType = rightJe.getType();
            TypeMirror type;
            // isSubtype() first does the cheaper test isSameType()
            if (types.isSubtype(leftType, rightType)) {
                type = rightType;
            } else if (types.isSubtype(rightType, leftType)) {
                type = leftType;
            } else if (expr.getOperator() == BinaryExpr.Operator.PLUS
                    && (types.isSameType(leftType, stringTypeMirror)
                            || types.isSameType(rightType, stringTypeMirror))) {
                type = stringTypeMirror;
            } else {
                throw new BugInCF("inconsistent types %s %s for %s", leftType, rightType, expr);
            }
            return new BinaryOperation(
                    type, javaParserBinaryOperatorToTreeKind(expr.getOperator()), leftJe, rightJe);
        }

        /**
         * Convert a JavaParser binary operator to a TreeKind.
         *
         * @param op a JavaParser binary operator
         * @return a TreeKind for the binary operator
         */
        Tree.Kind javaParserBinaryOperatorToTreeKind(BinaryExpr.Operator op) {
            switch (op) {
                case AND:
                    return Tree.Kind.CONDITIONAL_AND;
                case BINARY_AND:
                    return Tree.Kind.AND;
                case BINARY_OR:
                    return Tree.Kind.OR;
                case DIVIDE:
                    return Tree.Kind.DIVIDE;
                case EQUALS:
                    return Tree.Kind.EQUAL_TO;
                case GREATER:
                    return Tree.Kind.GREATER_THAN;
                case GREATER_EQUALS:
                    return Tree.Kind.GREATER_THAN_EQUAL;
                case LEFT_SHIFT:
                    return Tree.Kind.LEFT_SHIFT;
                case LESS:
                    return Tree.Kind.LESS_THAN;
                case LESS_EQUALS:
                    return Tree.Kind.LESS_THAN_EQUAL;
                case MINUS:
                    return Tree.Kind.MINUS;
                case MULTIPLY:
                    return Tree.Kind.MULTIPLY;
                case NOT_EQUALS:
                    return Tree.Kind.NOT_EQUAL_TO;
                case OR:
                    return Tree.Kind.CONDITIONAL_OR;
                case PLUS:
                    return Tree.Kind.PLUS;
                case REMAINDER:
                    return Tree.Kind.REMAINDER;
                case SIGNED_RIGHT_SHIFT:
                    return Tree.Kind.RIGHT_SHIFT;
                case UNSIGNED_RIGHT_SHIFT:
                    return Tree.Kind.UNSIGNED_RIGHT_SHIFT;
                case XOR:
                    return Tree.Kind.XOR;
                default:
                    throw new Error("unhandled " + op);
            }
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
                try {
                    return StaticJavaParser.parseExpression(type.asString())
                            .accept(this, context)
                            .getType();
                } catch (ParseProblemException e) {
                    return null;
                }
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
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(s, "no parameters found"));
            }
            int idx = Integer.parseInt(s.substring(PARAMETER_REPLACEMENT_LENGTH));

            if (idx == 0) {
                throw new ParseRuntimeException(
                        constructJavaExpressionParseError(
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
     * parsing; for instance, if "#2" appears within a string in s, then 2 is in the result list.
     * The result may contain duplicates.
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

    /**
     * Returns the 1-based index of the formal parameter that occurs in {@code s} or -1 if no formal
     * parameter occurs.
     *
     * @param s a Java expression
     * @return the 1-based indices of the formal parameter that occur in {@code s} or -1
     */
    public static int parameterIndex(String s) {
        Matcher matcher = ANCHORED_PARAMETER_PATTERN.matcher(s);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Contexts
    ///

    /**
     * Context used to parse and viewpoint-adapt a Java expression. It contains the JavaExpressions
     * to which {@code this} and the parameter syntax, e.g. {@code #1}, should parse.
     */
    public static class JavaExpressionContext {
        /** The value of {@code this} in this context. */
        public final JavaExpression receiver;
        /**
         * In a context for a method declaration or lambda, the formals. In a context for a method
         * invocation, the actuals. In other contexts, null.
         */
        public final List<JavaExpression> arguments;

        /** The checker. */
        public final SourceChecker checker;

        /**
         * Creates a context for parsing a Java expression, with "null" for arguments.
         *
         * @param receiver used to replace "this" in a Java expression and used to resolve
         *     identifiers in any Java expression with an implicit "this"
         * @param checker used to create {@link
         *     org.checkerframework.dataflow.expression.JavaExpression}s
         */
        public JavaExpressionContext(JavaExpression receiver, SourceChecker checker) {
            this(receiver, null, checker);
        }

        /**
         * Creates a context for parsing a Java expression.
         *
         * @param receiver used to replace "this" in a Java expression and used to resolve
         *     identifiers in any Java expression with an implicit "this"
         * @param arguments used to replace parameter references, e.g. #1, in Java expressions, null
         *     if no arguments
         * @param checker used to create {@link JavaExpression}s
         */
        public JavaExpressionContext(
                JavaExpression receiver, List<JavaExpression> arguments, SourceChecker checker) {
            assert checker != null;
            this.receiver = receiver;
            this.arguments = arguments;
            this.checker = checker;
        }

        /**
         * Creates a {@link JavaExpressionContext} for the method declared in {@code
         * methodDeclaration}.
         *
         * @param methodDeclaration used to translate parameter numbers in a Java expression to
         *     formal parameters of the method
         * @param checker used to build JavaExpression
         * @return context created from {@code methodDeclaration}
         */
        public static JavaExpressionContext buildContextForMethodDeclaration(
                MethodTree methodDeclaration, SourceChecker checker) {
            ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodDeclaration);
            JavaExpression thisExpression = JavaExpression.getImplicitReceiver(methodElt);
            List<JavaExpression> parametersJe =
                    SystemUtil.mapList(LocalVariable::new, methodElt.getParameters());
            return new JavaExpressionContext(thisExpression, parametersJe, checker);
        }

        /**
         * Creates a {@link JavaExpressionContext} for the given lambda.
         *
         * @param lambdaTree a lambda
         * @param path the path to the lambda
         * @param checker used to build JavaExpression
         * @return context created for {@code lambdaTree}
         */
        public static JavaExpressionContext buildContextForLambda(
                LambdaExpressionTree lambdaTree, TreePath path, SourceChecker checker) {
            TypeMirror enclosingType = TreeUtils.typeOf(TreePathUtil.enclosingClass(path));
            JavaExpression receiverJe = new ThisReference(enclosingType);
            List<JavaExpression> parametersJe =
                    SystemUtil.mapList(
                            JavaExpression::fromVariableTree, lambdaTree.getParameters());
            return new JavaExpressionContext(receiverJe, parametersJe, checker);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the class {@code classTree} as seen at the
         * class declaration.
         *
         * @param classTree a class
         * @param checker used to build JavaExpression
         * @return a {@link JavaExpressionContext} for the class {@code classTree} as seen at the
         *     class declaration
         */
        public static JavaExpressionContext buildContextForClassDeclaration(
                ClassTree classTree, SourceChecker checker) {
            JavaExpression receiverJe = new ThisReference(TreeUtils.typeOf(classTree));
            return new JavaExpressionContext(receiverJe, Collections.emptyList(), checker);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the method called by {@code
         * methodInvocation}, as seen at the method use (i.e., at the call site).
         *
         * @param methodInvocation a method invocation
         * @param checker the javac components to use
         * @return a {@link JavaExpressionContext} for the method {@code methodInvocation}
         */
        public static JavaExpressionContext buildContextForMethodUse(
                MethodInvocationNode methodInvocation, SourceChecker checker) {
            Node receiver = methodInvocation.getTarget().getReceiver();
            JavaExpression receiverJe = JavaExpression.fromNode(receiver);
            List<JavaExpression> argumentsJe =
                    SystemUtil.mapList(JavaExpression::fromNode, methodInvocation.getArguments());
            return new JavaExpressionContext(receiverJe, argumentsJe, checker);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the method called by {@code
         * methodInvocation}, as seen at the method use (i.e., at the call site).
         *
         * @param methodInvocation a method invocation
         * @param checker the javac components to use
         * @return a {@link JavaExpressionContext} for the method {@code methodInvocation}
         */
        public static JavaExpressionContext buildContextForMethodUse(
                MethodInvocationTree methodInvocation, SourceChecker checker) {
            JavaExpression receiverJe = JavaExpression.getReceiver(methodInvocation);

            List<? extends ExpressionTree> args = methodInvocation.getArguments();
            List<JavaExpression> argumentsJe = SystemUtil.mapList(JavaExpression::fromTree, args);

            return new JavaExpressionContext(receiverJe, argumentsJe, checker);
        }

        /**
         * Returns a {@link JavaExpressionContext} for the constructor {@code n} (represented as a
         * {@link Node} as seen at the constructor use (i.e., at a "new" expression).
         *
         * @param n an object creation node
         * @param checker the checker
         * @return a {@link JavaExpressionContext} for the constructor {@code n} (represented as a
         *     {@link Node} as seen at the constructor use (i.e., at a "new" expression)
         */
        public static JavaExpressionContext buildContextForNewClassUse(
                ObjectCreationNode n, SourceChecker checker) {

            // This returns an Unknown with the type set to the class in which the
            // constructor is declared
            JavaExpression receiverJe = JavaExpression.fromNode(n);

            List<JavaExpression> argumentsJe =
                    SystemUtil.mapList(JavaExpression::fromNode, n.getArguments());

            return new JavaExpressionContext(receiverJe, argumentsJe, checker);
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
            sj.add("checker=" + "...");
            // sj.add("checker="+ checker);
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

    ///////////////////////////////////////////////////////////////////////////
    /// Exceptions
    ///

    /**
     * An exception that indicates a parse error. Call {@link #getDiagMessage} to obtain a {@link
     * DiagMessage} that can be used for error reporting.
     */
    public static class JavaExpressionParseException extends Exception {
        private static final long serialVersionUID = 2L;
        /** The error message key. */
        private @CompilerMessageKey String errorKey;
        /** The arguments to the error message key. */
        public final Object[] args;

        /**
         * Create a new JavaExpressionParseException.
         *
         * @param errorKey the error message key
         * @param args the arguments to the error message key
         */
        public JavaExpressionParseException(@CompilerMessageKey String errorKey, Object... args) {
            this(null, errorKey, args);
        }

        /**
         * Create a new JavaExpressionParseException.
         *
         * @param cause cause
         * @param errorKey the error message key
         * @param args the arguments to the error message key
         */
        public JavaExpressionParseException(
                @Nullable Throwable cause, @CompilerMessageKey String errorKey, Object... args) {
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
     *
     * @param expr the string that could not be parsed
     * @param explanation an explanation of the parse failure
     * @return a {@link JavaExpressionParseException} for the expression {@code expr} with
     *     explanation {@code explanation}.
     */
    private static JavaExpressionParseException constructJavaExpressionParseError(
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
