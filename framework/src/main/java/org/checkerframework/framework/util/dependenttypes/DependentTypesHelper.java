package org.checkerframework.framework.util.dependenttypes;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.expression.ArrayCreation;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.visitor.AnnotatedTypeComparer;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.StringsPlume;

/**
 * A class that helps checkers use qualifiers that are represented by annotations with Java
 * expression strings. This class performs four main functions:
 *
 * <ol>
 *   <li>Standardizes/canonicalizes the expressions in the annotations such that two expression
 *       strings that are equivalent are made to be equal. For example, an instance field f may
 *       appear in an expression string as "f" or "this.f"; this class standardizes both strings to
 *       "this.f". It also standardizes formal parameter references such as "#2" to the formal
 *       parameter name.
 *   <li>Viewpoint-adapts annotations on field or method declarations at field accesses or method
 *       invocations.
 *   <li>Changes invalid expression strings to an error string that includes the reason why the
 *       expression is invalid. For example, {@code @KeyFor("m")} would be changed to
 *       {@code @KeyFor("[error for expression: m error: m: identifier not found]")} if m is not a
 *       valid identifier. This allows subtyping checks to assume that if two strings are equal and
 *       not errors, the reference the same valid Java expression.
 *   <li>Checks annotated types for error strings that have been added by this class and issues an
 *       error if any are found.
 * </ol>
 *
 * <p>Steps 3 and 4 are separated so that an error is issued only once per invalid expression string
 * rather than every time the expression string is parsed. (The expression string is parsed multiple
 * times because annotated types are created multiple times.)
 */
public class DependentTypesHelper {
    protected final AnnotatedTypeFactory factory;

    /** A map of annotation classes to the names of their elements that are Java expressions. */
    private Map<Class<? extends Annotation>, List<String>> annoToElements;

    /**
     * The {@code ExpressionErrorChecker} that scans an annotated type and returns a list of {@link
     * DependentTypesError}.
     */
    private final ExpressionErrorChecker expressionErrorChecker;

    /**
     * A scanner that standardizes Java expression strings in dependent type annotations. Call
     * {@code StandardizeTypeAnnotator#init(JavaExpressionContext, TreePath, boolean, boolean)}
     * before each use.
     */
    private final StandardizeTypeAnnotator standardizeTypeAnnotator;

    /**
     * Copies annotations that might have been viewpoint adapted from the visited type (the first
     * formal parameter of the {@code ViewpointAdaptedCopier#visit}) to the second formal parameter.
     */
    private final ViewpointAdaptedCopier viewpointAdaptedCopier;

    /**
     * Creates {@code DependentTypesHelper}.
     *
     * @param factory annotated type factory
     */
    public DependentTypesHelper(AnnotatedTypeFactory factory) {
        this.factory = factory;

        this.annoToElements = new HashMap<>();
        for (Class<? extends Annotation> expressionAnno : factory.getSupportedTypeQualifiers()) {
            List<String> elementList = getExpressionElementNames(expressionAnno);
            if (elementList != null && !elementList.isEmpty()) {
                annoToElements.put(expressionAnno, elementList);
            }
        }
        this.expressionErrorChecker = new ExpressionErrorChecker();
        this.standardizeTypeAnnotator = new StandardizeTypeAnnotator();
        this.viewpointAdaptedCopier = new ViewpointAdaptedCopier();
    }

    /**
     * Returns true if any qualifier in the type system is a dependent type annotation.
     *
     * @return true if any qualifier in the type system is a dependent type annotation
     */
    public boolean hasDependentAnnotations() {
        return !annoToElements.isEmpty();
    }

    /**
     * Returns a list of the names of elements in the annotation class that should be interpreted as
     * Java expressions, namely those annotated with {@code @}{@link JavaExpression}.
     *
     * @param clazz annotation class
     * @return a list of the names of elements in the annotation class that should be interpreted as
     *     Java expressions
     */
    private static List<String> getExpressionElementNames(Class<? extends Annotation> clazz) {
        Method[] methods = clazz.getMethods();
        if (methods == null) {
            return Collections.emptyList();
        }
        List<String> elements = new ArrayList<>();
        for (Method method : methods) {
            org.checkerframework.framework.qual.JavaExpression javaExpression =
                    method.getAnnotation(org.checkerframework.framework.qual.JavaExpression.class);
            if (javaExpression != null) {
                elements.add(method.getName());
            }
        }
        return elements;
    }

    ///
    /// Viewpoint adaptation
    ///

    /**
     * Viewpoint-adapts the dependent type annotations on the bounds to the use of the type.
     *
     * @param classDecl class or interface declaration whose type variables should be viewpoint
     *     adapted
     * @param bounds annotated types of the bounds of the type variables; side-effected by this
     *     method
     * @param pathToUse tree path to the use of the class or interface
     */
    public void viewpointAdaptTypeVariableBounds(
            TypeElement classDecl, List<AnnotatedTypeParameterBounds> bounds, TreePath pathToUse) {
        if (!hasDependentAnnotations()) {
            return;
        }
        JavaExpression r = JavaExpression.getImplicitReceiver(classDecl);
        JavaExpressionContext context = new JavaExpressionContext(r, factory.getChecker());
        for (AnnotatedTypeParameterBounds bound : bounds) {
            standardizeDoNotUseLocalScope(context, pathToUse, bound.getUpperBound());
            standardizeDoNotUseLocalScope(context, pathToUse, bound.getLowerBound());
        }
    }

    /**
     * Viewpoint-adapts the dependent type annotations in the methodDeclType based on the
     * methodInvocationTree.
     *
     * @param methodInvocationTree use of the method
     * @param methodDeclType type of the method declaration; is side-effected by this method
     */
    public void viewpointAdaptMethod(
            MethodInvocationTree methodInvocationTree, AnnotatedExecutableType methodDeclType) {
        if (!hasDependentAnnotations()) {
            return;
        }
        List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
        viewpointAdaptExecutable(methodInvocationTree, methodDeclType, args);
    }

    /**
     * Viewpoint-adapts the dependent type annotations in the constructorType based on the
     * newClassTree.
     *
     * @param newClassTree invocation of the constructor
     * @param constructorType type of the constructor; is side-effected by this method
     */
    public void viewpointAdaptConstructor(
            NewClassTree newClassTree, AnnotatedExecutableType constructorType) {
        if (!hasDependentAnnotations()) {
            return;
        }
        List<? extends ExpressionTree> args = newClassTree.getArguments();
        viewpointAdaptExecutable(newClassTree, constructorType, args);
    }

    /**
     * Viewpoint-adapts a method or constructor invocation.
     *
     * @param tree invocation of the method or constructor
     * @param methodType type of the method or constructor; is side-effected by this method
     * @param argTrees the arguments to the method or constructor; subexpressions of {@code tree}
     */
    private void viewpointAdaptExecutable(
            ExpressionTree tree,
            AnnotatedExecutableType methodType,
            List<? extends ExpressionTree> argTrees) {
        assert hasDependentAnnotations();
        Element methodElt = TreeUtils.elementFromUse(tree);
        // The annotations on `viewpointAdaptedType` will be copied to `methodType`.
        AnnotatedExecutableType viewpointAdaptedType =
                (AnnotatedExecutableType) factory.getAnnotatedType(methodElt);
        if (!hasDependentType(viewpointAdaptedType)) {
            return;
        }

        JavaExpression receiver = JavaExpression.getReceiver(tree, factory);
        List<JavaExpression> argsJe = argumentTreesToJavaExpressions(tree, methodType, argTrees);

        TreePath currentPath = factory.getPath(tree);

        JavaExpressionContext context =
                new JavaExpressionContext(receiver, argsJe, factory.getChecker());

        // methodType cannot be viewpoint adapted directly because it is the type post type variable
        // substitution.  Dependent type annotations on type arguments cannot be
        // viewpoint adapted along with the dependent type annotations that are on the method
        // declaration. For example:
        //   Map<String, String> map = ...;
        //   List<@KeyFor("map") String> list = ...;
        //   list.get(0)
        // If the type of List.get is viewpoint adapted for the invocation "list.get(0)", then
        // methodType would be @KeyFor("map") String get(int).
        //
        // Instead, use the type for the method (viewpointAdaptedType) and viewpoint adapt that
        // type.
        // Then copy annotations from the viewpoint adapted type to methodType, if that annotation
        // is not on a type that was substituted for a type variable.

        standardizeDoNotUseLocalScope(context, currentPath, viewpointAdaptedType);
        this.viewpointAdaptedCopier.visit(viewpointAdaptedType, methodType);
    }

    /**
     * Converts method or constructor arguments from Trees to JavaExpressions, accounting for
     * varargs.
     *
     * @param tree invocation of the method or constructor
     * @param methodType type of the method or constructor
     * @param argTrees the arguments to the method or constructor; subexpressions of {@code tree}
     * @return the arguments, as JavaExpressions
     */
    private List<JavaExpression> argumentTreesToJavaExpressions(
            ExpressionTree tree,
            AnnotatedExecutableType methodType,
            List<? extends ExpressionTree> argTrees) {

        if (tree.getKind() == Kind.METHOD_INVOCATION) {
            ExecutableElement method = TreeUtils.elementFromUse((MethodInvocationTree) tree);
            if (isVarArgsInvocation(method, methodType, argTrees)) {
                List<JavaExpression> result = new ArrayList<>();

                for (int i = 0; i < method.getParameters().size() - 1; i++) {
                    result.add(JavaExpression.fromTree(factory, argTrees.get(i)));
                }
                List<JavaExpression> varargArgs = new ArrayList<>();
                for (int i = method.getParameters().size() - 1; i < argTrees.size(); i++) {
                    varargArgs.add(JavaExpression.fromTree(factory, argTrees.get(i)));
                }
                Element varargsElement =
                        method.getParameters().get(method.getParameters().size() - 1);
                TypeMirror tm = ElementUtils.getType(varargsElement);
                result.add(new ArrayCreation(tm, Collections.emptyList(), varargArgs));

                return result;
            }
        }

        List<JavaExpression> result = new ArrayList<>();
        for (ExpressionTree argTree : argTrees) {
            result.add(JavaExpression.fromTree(factory, argTree));
        }
        return result;
    }

    /**
     * Returns true if method is a varargs method or constructor and its varargs arguments are not
     * passed in an array.
     *
     * @param method the method or constructor
     * @param methodType type of the method or constructor; used for determining the type of the
     *     varargs formal parameter
     * @param args the arguments at the call site
     * @return true if method is a varargs method and its varargs arguments are not passed in an
     *     array
     */
    private boolean isVarArgsInvocation(
            ExecutableElement method,
            AnnotatedExecutableType methodType,
            List<? extends ExpressionTree> args) {
        if (method != null && method.isVarArgs()) {
            if (method.getParameters().size() != args.size()) {
                return true;
            }
            AnnotatedTypeMirror lastArg = factory.getAnnotatedType(args.get(args.size() - 1));
            List<AnnotatedTypeMirror> paramTypes = methodType.getParameterTypes();
            AnnotatedArrayType lastParam =
                    (AnnotatedArrayType) paramTypes.get(paramTypes.size() - 1);
            return lastArg.getKind() != TypeKind.ARRAY
                    || AnnotatedTypes.getArrayDepth(lastParam)
                            != AnnotatedTypes.getArrayDepth((AnnotatedArrayType) lastArg);
        }
        return false;
    }

    ///
    /// Standardization
    ///

    /**
     * Creates a TreeAnnotator that standardizes dependent type annotations.
     *
     * @return a new TreeAnnotator that standardizes dependent type annotations
     */
    public TreeAnnotator createDependentTypesTreeAnnotator() {
        assert hasDependentAnnotations();
        return new DependentTypesTreeAnnotator(factory, this);
    }

    /**
     * Standardizes the Java expressions in annotations on a constructor invocation.
     *
     * @param tree the constructor invocation
     * @param type the type of the expression; is side-effected by this method
     */
    public void standardizeNewClassTree(NewClassTree tree, AnnotatedDeclaredType type) {
        if (!hasDependentType(type)) {
            return;
        }

        TreePath path = factory.getPath(tree);
        Tree enclosingClass = TreePathUtil.enclosingClass(path);
        TypeMirror enclosingType = TreeUtils.typeOf(enclosingClass);
        JavaExpression r = JavaExpression.getPseudoReceiver(path, enclosingType);
        JavaExpressionContext context =
                new JavaExpressionContext(
                        r,
                        JavaExpression.getParametersOfEnclosingMethod(factory, path),
                        factory.getChecker());
        standardizeUseLocalScope(context, path, type);
    }

    /**
     * Standardize the Java expressions in annotations in a class declaration.
     *
     * @param node the class declaration
     * @param type the type of the class declaration; is side-effected by this method
     * @param classElt the element of the class declaration
     */
    public void standardizeClass(ClassTree node, AnnotatedTypeMirror type, Element classElt) {
        if (!hasDependentType(type)) {
            return;
        }

        TreePath path = factory.getPath(node);
        if (path == null) {
            return;
        }
        JavaExpression receiverJe = JavaExpression.getImplicitReceiver(classElt);
        JavaExpressionContext classignmentContext =
                new JavaExpressionContext(receiverJe, factory.getChecker());
        standardizeDoNotUseLocalScope(classignmentContext, path, type);
    }

    /**
     * Standardizes the Java expressions in annotations for a method return type. {@code atm} might
     * come from the method declaration or from the type of the expression in a {@code return}
     * statement.
     *
     * @param methodDeclTree a method declaration
     * @param atm the method return type; is side-effected by this method
     */
    public void standardizeReturnType(MethodTree methodDeclTree, AnnotatedTypeMirror atm) {
        if (!hasDependentType(atm)) {
            return;
        }

        TreePath pathToMethodDecl = factory.getPath(methodDeclTree);
        if (pathToMethodDecl == null) {
            return;
        }

        JavaExpressionContext context =
                JavaExpressionContext.buildContextForMethodDeclaration(
                        methodDeclTree, pathToMethodDecl, factory.getChecker());
        standardizeDoNotUseLocalScope(context, pathToMethodDecl, atm);
    }

    /**
     * Viewpoint-adapt all dependent type annotations to the method declaration, {@code
     * methodDeclTree}. This changes occurrences of formal parameter names to "#2" syntax, and it
     * removes expressions that contain other local variables.
     *
     * @param methodDeclTree the method declaration to which the annotations are viewpoint-adapted
     * @param atm type to viewpoint-adapt; is side-effected by this method
     */
    public void delocalize(MethodTree methodDeclTree, AnnotatedTypeMirror atm) {
        if (!hasDependentType(atm)) {
            return;
        }

        TreePath pathToMethodDecl = factory.getPath(methodDeclTree);
        if (pathToMethodDecl == null) {
            return;
        }
        // TODO: 1.) parameter names need to be coverted to the # index syntax.
        // TODO: 2.) If an annotation only has expressions that cannot be delocalized, then that
        // annotation needs to be changed to top, rather than the dependent type annotation with
        // an empty array as a value element.

        JavaExpressionContext context =
                JavaExpressionContext.buildContextForMethodDeclaration(
                        methodDeclTree, pathToMethodDecl, factory.getChecker());
        standardizeAtm(
                context,
                pathToMethodDecl,
                atm,
                /*useLocalScope=*/ false, /*removeErroneousExpressions*/
                true);
    }

    /** A set containing {@link Tree.Kind#METHOD} and {@link Tree.Kind#LAMBDA_EXPRESSION}. */
    private static final Set<Tree.Kind> METHOD_OR_LAMBDA =
            EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);

    /**
     * Standardize the Java expressions in annotations in a variable declaration.
     *
     * @param declarationTree the variable declaration
     * @param type the type of the variable declaration
     * @param variableElt the element of the variable declaration
     */
    public void standardizeVariable(
            Tree declarationTree, AnnotatedTypeMirror type, Element variableElt) {
        if (!hasDependentType(type)) {
            return;
        }

        TreePath pathToVariableDecl = factory.getPath(declarationTree);
        if (pathToVariableDecl == null) {
            return;
        }
        switch (variableElt.getKind()) {
            case PARAMETER:
                TreePath pathTillEnclTree =
                        TreePathUtil.pathTillOfKind(pathToVariableDecl, METHOD_OR_LAMBDA);
                if (pathTillEnclTree == null) {
                    throw new BugInCF("no enclosing method or lambda found");
                }
                Tree enclTree = pathTillEnclTree.getLeaf();

                if (enclTree.getKind() == Kind.METHOD) {
                    MethodTree methodDeclTree = (MethodTree) enclTree;
                    JavaExpressionContext context =
                            JavaExpressionContext.buildContextForMethodDeclaration(
                                    methodDeclTree, pathTillEnclTree, factory.getChecker());
                    standardizeDoNotUseLocalScope(context, pathTillEnclTree, type);
                } else {
                    LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclTree;
                    JavaExpressionContext parameterContext =
                            JavaExpressionContext.buildContextForLambda(
                                    lambdaTree, pathToVariableDecl, factory.getChecker());
                    // Lambdas can use local variables defined in the enclosing method, so allow
                    // identifiers to be locals in scope at the location of the lambda.
                    standardizeUseLocalScope(
                            parameterContext, pathToVariableDecl.getParentPath(), type);
                }
                break;

            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                TypeMirror enclosingType = ElementUtils.enclosingTypeElement(variableElt).asType();
                JavaExpression receiver =
                        JavaExpression.getPseudoReceiver(pathToVariableDecl, enclosingType);
                List<JavaExpression> params =
                        JavaExpression.getParametersOfEnclosingMethod(factory, pathToVariableDecl);
                JavaExpressionContext localContext =
                        new JavaExpressionContext(receiver, params, factory.getChecker());
                standardizeUseLocalScope(localContext, pathToVariableDecl, type);
                break;

            case FIELD:
            case ENUM_CONSTANT:
                JavaExpression receiverJe;
                if (declarationTree.getKind() == Tree.Kind.IDENTIFIER) {
                    JavaExpression nodeJe =
                            JavaExpression.fromTree(factory, (IdentifierTree) declarationTree);
                    receiverJe =
                            nodeJe instanceof FieldAccess
                                    ? ((FieldAccess) nodeJe).getReceiver()
                                    : nodeJe;
                } else {
                    receiverJe = JavaExpression.getImplicitReceiver(variableElt);
                }
                JavaExpressionContext fieldContext =
                        new JavaExpressionContext(receiverJe, factory.getChecker());
                standardizeDoNotUseLocalScope(fieldContext, pathToVariableDecl, type);
                break;

            default:
                throw new BugInCF(
                        "unexpected element kind " + variableElt.getKind() + " for " + variableElt);
        }
    }

    /**
     * Standardize the Java expressions in annotations in a field access.
     *
     * @param node a field access
     * @param type its type; is side-effected by this method
     */
    public void standardizeFieldAccess(MemberSelectTree node, AnnotatedTypeMirror type) {
        if (!hasDependentType(type)) {
            return;
        }

        if (TreeUtils.isClassLiteral(node)) {
            return;
        }
        Element ele = TreeUtils.elementFromUse(node);
        if (ele.getKind() != ElementKind.FIELD) {
            return;
        }

        JavaExpression receiver = JavaExpression.fromTree(factory, node.getExpression());
        JavaExpressionContext context = new JavaExpressionContext(receiver, factory.getChecker());
        standardizeDoNotUseLocalScope(context, factory.getPath(node), type);
    }

    /**
     * Standardize the Java expressions in annotations in the type of an expression.
     *
     * @param tree an expression
     * @param annotatedType its type; is side-effected by this method
     */
    public void standardizeExpression(ExpressionTree tree, AnnotatedTypeMirror annotatedType) {
        if (!hasDependentType(annotatedType)) {
            return;
        }

        TreePath path = factory.getPath(tree);
        if (path == null) {
            return;
        }
        Tree enclosingClass = TreePathUtil.enclosingClass(path);
        TypeMirror enclosingType = TreeUtils.typeOf(enclosingClass);

        JavaExpression receiver = JavaExpression.getPseudoReceiver(path, enclosingType);

        JavaExpressionContext localContext =
                new JavaExpressionContext(
                        receiver,
                        JavaExpression.getParametersOfEnclosingMethod(factory, path),
                        factory.getChecker());
        standardizeUseLocalScope(localContext, path, annotatedType);
    }

    /**
     * Standardize the Java expressions in annotations in a type.
     *
     * @param type the type to standardize
     * @param elt the element whose type is {@code type}
     */
    public void standardizeVariable(AnnotatedTypeMirror type, Element elt) {
        if (!hasDependentType(type)) {
            return;
        }

        switch (elt.getKind()) {
            case PARAMETER:
            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                Tree declarationTree = factory.declarationFromElement(elt);
                if (declarationTree == null) {
                    if (elt.getKind() == ElementKind.PARAMETER) {
                        // The tree might be null when
                        // org.checkerframework.framework.flow.CFAbstractTransfer.getValueFromFactory()
                        // gets the assignment context for a pseudo assignment of an argument to
                        // a method parameter.
                        return;
                    }
                    throw new BugInCF(this.getClass() + ": tree not found");
                } else if (TreeUtils.typeOf(declarationTree) == null) {
                    // org.checkerframework.framework.flow.CFAbstractTransfer.getValueFromFactory()
                    // gets the assignment context for a pseudo assignment of an argument to
                    // a method parameter.
                    return;
                }

                standardizeVariable(declarationTree, type, elt);
                return;

            default:
                // It's not a variable (it might be METHOD, CONSTRUCTOR, CLASS, or INTERFACE, for
                // example), so there is nothing to do.
                break;
        }
    }

    /**
     * Standardize a type, setting useLocalScope to true.
     *
     * @param context the context
     * @param localScope the local scope
     * @param type the type to standardize; is side-effected by this method
     */
    private void standardizeUseLocalScope(
            JavaExpressionContext context, TreePath localScope, AnnotatedTypeMirror type) {
        standardizeAtm(context, localScope, type, /*useLocalScope=*/ true);
    }

    // TODO: Eliminate all uses of this.
    /**
     * Standardize a type, setting useLocalScope to false.
     *
     * @param context the context
     * @param localScope the local scope
     * @param type the type to standardize; is side-effected by this method
     */
    private void standardizeDoNotUseLocalScope(
            JavaExpressionContext context, TreePath localScope, AnnotatedTypeMirror type) {
        standardizeAtm(
                context,
                localScope,
                type,
                /*useLocalScope=*/ false,
                /*removeErroneousExpressions=*/ false);
    }

    private void standardizeAtm(
            JavaExpressionContext context,
            TreePath localScope,
            AnnotatedTypeMirror type,
            boolean useLocalScope) {
        standardizeAtm(
                context, localScope, type, useLocalScope, /*removeErroneousExpressions=*/ false);
    }

    /**
     * @param removeErroneousExpressions if true, remove erroneous expressions rather than
     *     converting them into an explanation of why they are illegal
     */
    private void standardizeAtm(
            JavaExpressionContext context,
            TreePath localScope,
            AnnotatedTypeMirror type,
            boolean useLocalScope,
            boolean removeErroneousExpressions) {
        // localScope is null in dataflow when creating synthetic trees for enhanced for loops.
        if (localScope == null) {
            return;
        }
        this.standardizeTypeAnnotator.init(
                context, localScope, useLocalScope, removeErroneousExpressions);
        this.standardizeTypeAnnotator.visit(type);
    }

    /**
     * Standardizes a Java expression.
     *
     * @param expression a Java expression
     * @param context the context
     * @param localScope the local scope
     * @param useLocalScope whether {@code localScope} should be used to resolve identifiers
     * @return the standardized version of the Java expression
     */
    protected String standardizeString(
            String expression,
            JavaExpressionContext context,
            TreePath localScope,
            boolean useLocalScope) {
        if (DependentTypesError.isExpressionError(expression)) {
            return expression;
        }
        JavaExpression result;
        try {
            result = JavaExpressionParseUtil.parse(expression, context, localScope, useLocalScope);
        } catch (JavaExpressionParseUtil.JavaExpressionParseException e) {
            return new DependentTypesError(expression, e).toString();
        }
        if (result == null) {
            return new DependentTypesError(expression, /*error message=*/ " ").toString();
        }
        // Replace references to compile-time constant fields by the constant itself.  (This is only
        // desirable if the name doesn't matter.  The name matters for @KeyFor and @GuardedBy, but
        // they are not relevant to primitives.)
        if (result instanceof FieldAccess && ((FieldAccess) result).isFinal()) {
            Object constant = ((FieldAccess) result).getField().getConstantValue();
            if (constant != null && !(constant instanceof String)) {
                return constant.toString();
            }
        }
        return result.toString();
    }

    /**
     * Standardizes Java expressions in an annotation. If the annotation is not a dependent type
     * annotation, returns null.
     *
     * @param context information about any receiver and arguments
     * @param localScope path to local scope to use
     * @param anno the annotation to be standardized
     * @param useLocalScope whether the local scope should be used to resolve identifiers
     * @param removeErroneousExpressions if true, remove erroneous expressions rather than
     *     converting them into an explanation of why they are illegal
     * @return the standardized annotation, or null if no standardization is needed
     */
    public AnnotationMirror standardizeAnnotationIfDependentType(
            JavaExpressionContext context,
            TreePath localScope,
            AnnotationMirror anno,
            boolean useLocalScope,
            boolean removeErroneousExpressions) {
        if (!isExpressionAnno(anno)) {
            return null;
        }
        return standardizeDependentTypeAnnotation(
                context, localScope, anno, useLocalScope, removeErroneousExpressions);
    }

    /**
     * Standardizes a dependent type annotation. Returns a new annotation.
     *
     * @param anno a dependent type annotation
     * @param removeErroneousExpressions if true, remove erroneous expressions rather than
     *     converting them into an explanation of why they are illegal
     */
    private AnnotationMirror standardizeDependentTypeAnnotation(
            JavaExpressionContext context,
            TreePath localScope,
            AnnotationMirror anno,
            boolean useLocalScope,
            boolean removeErroneousExpressions) {
        AnnotationBuilder builder =
                new AnnotationBuilder(
                        factory.getProcessingEnv(), AnnotationUtils.annotationName(anno));

        for (String value : getListOfExpressionElements(anno)) {
            List<String> expressionStrings =
                    AnnotationUtils.getElementValueArray(anno, value, String.class, true);
            List<String> standardizedStrings = new ArrayList<>();
            for (String expression : expressionStrings) {
                String standardized =
                        standardizeString(expression, context, localScope, useLocalScope);
                if (removeErroneousExpressions
                        && DependentTypesError.isExpressionError(standardized)) {
                    // nothing to do
                } else {
                    standardizedStrings.add(standardized);
                }
            }
            builder.setValue(value, standardizedStrings);
        }
        return builder.build();
    }

    /** A scanner that standardizes Java expression strings in dependent type annotations. */
    private class StandardizeTypeAnnotator extends AnnotatedTypeScanner<Void, Void> {
        /** The context. */
        private JavaExpressionContext context;
        /** The local scope. */
        private TreePath localScope;
        /**
         * Whether or not the expression might contain a variable declared in local scope. Really,
         * whether to use {@code localScope} to resolve identifiers.
         */
        private boolean useLocalScope;
        /**
         * If true, remove erroneous expressions. If false, replace them by an explanation of why
         * they are illegal.
         */
        private boolean removeErroneousExpressions;

        /**
         * Constructs a {@code StandardizeTypeAnnotator} with all fields set to null. Call {@code
         * #init(JavaExpressionContext, TreePath, boolean, boolean)} before scanning.
         */
        private StandardizeTypeAnnotator() {
            this.context = null;
            this.localScope = null;
            this.useLocalScope = false;
            this.removeErroneousExpressions = false;
        }

        /**
         * Initialize the scanner to standardize with respect to the given context.
         *
         * @param context JavaExpressionContext
         * @param localScope tree path for local scope
         * @param useLocalScope whether or not to use locals
         * @param removeErroneousExpressions removeErroneousExpressions if true, remove erroneous
         *     expressions rather than converting them into an explanation of why they are illegal
         */
        private void init(
                JavaExpressionContext context,
                TreePath localScope,
                boolean useLocalScope,
                boolean removeErroneousExpressions) {
            this.context = context;
            this.localScope = localScope;
            this.useLocalScope = useLocalScope;
            this.removeErroneousExpressions = removeErroneousExpressions;
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeMirror.AnnotatedTypeVariable type, Void aVoid) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, null);

            // If the type variable has a primary annotation, then it is viewpoint adapted then
            // copied to the upper and lower bounds.  Attempting to viewpoint adapt again, could
            // cause the JavaExpression parser to fail.  So, remove the primary annotations from
            // the upper and lower bound before they are recursively visited.  Then add them back.
            Set<AnnotationMirror> primarys = type.getAnnotations();
            type.getLowerBound().removeAnnotations(primarys);
            Void r = scan(type.getLowerBound(), aVoid);
            type.getLowerBound().addAnnotations(primarys);
            visitedNodes.put(type, r);

            type.getUpperBound().removeAnnotations(primarys);
            r = scanAndReduce(type.getUpperBound(), aVoid, r);
            type.getUpperBound().addAnnotations(primarys);
            visitedNodes.put(type, r);
            return r;
        }

        @Override
        protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
            for (AnnotationMirror anno :
                    AnnotationUtils.createAnnotationSet(type.getAnnotations())) {
                AnnotationMirror newAnno =
                        standardizeAnnotationIfDependentType(
                                context,
                                localScope,
                                anno,
                                useLocalScope,
                                removeErroneousExpressions);
                if (newAnno != null) {
                    // Standardized annotations are written into bytecode along with explicitly
                    // written nonstandard annotations. (This is a bug.)
                    // Remove the old annotation and add the new annotations. The new annotation for
                    // both the nonstandard annotation and the standard annotation are equal with
                    // respect to Object#equals, so only one new annotation will be added to the
                    // type.
                    type.removeAnnotation(anno);
                    type.addAnnotation(newAnno);
                }
            }
            return super.scan(type, aVoid);
        }
    }

    /**
     * Checks all Java expressions in the given annotated type to see if the expression string is an
     * error string as specified by {@link DependentTypesError#isExpressionError}. If the annotated
     * type has any errors, an expression.unparsable.type.invalid error is issued at the errorTree.
     *
     * @param atm annotated type to check for expression errors
     * @param errorTree the tree at which to report any found errors
     */
    public void checkType(AnnotatedTypeMirror atm, Tree errorTree) {
        if (!hasDependentAnnotations()) {
            return;
        }

        List<DependentTypesError> errors = expressionErrorChecker.visit(atm);
        if (errors == null || errors.isEmpty()) {
            return;
        }
        if (errorTree.getKind() == Kind.VARIABLE) {
            ModifiersTree modifiers = ((VariableTree) errorTree).getModifiers();
            errorTree = ((VariableTree) errorTree).getType();
            for (AnnotationTree annoTree : modifiers.getAnnotations()) {
                for (Class<?> annoClazz : annoToElements.keySet()) {
                    // TODO: Simple string containment seems too simplistic.  At least check for a
                    // word boundary.
                    if (annoTree.toString().contains(annoClazz.getSimpleName())) {
                        errorTree = annoTree;
                        break;
                    }
                }
            }
        }
        reportErrors(errorTree, errors);
    }

    /**
     * Report the given errors as "expression.unparsable.type.invalid".
     *
     * @param errorTree where to report the errors
     * @param errors the errors to report
     */
    protected void reportErrors(Tree errorTree, List<DependentTypesError> errors) {
        SourceChecker checker = factory.getChecker();
        for (DependentTypesError dte : errors) {
            checker.reportError(errorTree, "expression.unparsable.type.invalid", dte.format());
        }
    }

    /**
     * Returns all the Java expression elements of the annotation that are an error string as
     * specified by DependentTypesError#isExpressionError.
     *
     * @param am an annotation
     * @return the elements of {@code am} that are errors
     */
    private List<DependentTypesError> errorElements(AnnotationMirror am) {
        assert hasDependentAnnotations();

        List<DependentTypesError> errors = new ArrayList<>();

        for (String element : getListOfExpressionElements(am)) {
            List<String> value =
                    AnnotationUtils.getElementValueArray(am, element, String.class, true);
            for (String v : value) {
                if (DependentTypesError.isExpressionError(v)) {
                    errors.add(DependentTypesError.unparse(v));
                }
            }
        }
        return errors;
    }

    /**
     * Checks every Java expression element of the annotation to see if the expression is an error
     * string as specified by DependentTypesError#isExpressionError. If any expression is an error,
     * then a flowexpr.parse.error error is reported at {@code errorTree}.
     *
     * @param annotation annotation to check
     * @param errorTree location at which to issue errors
     */
    public void checkAnnotation(AnnotationMirror annotation, Tree errorTree) {
        if (!hasDependentAnnotations()) {
            return;
        }

        List<DependentTypesError> errors = errorElements(annotation);
        if (errors.isEmpty()) {
            return;
        }
        SourceChecker checker = factory.getChecker();
        String error = StringsPlume.joinLines(errors);
        checker.reportError(errorTree, "flowexpr.parse.error", error);
    }

    /**
     * Checks all Java expressions in the class declaration AnnotatedTypeMirror to see if the
     * expression string is an error string as specified by DependentTypesError#isExpressionError.
     * If the annotated type has any errors, a flowexpr.parse.error is issued. Note that this checks
     * the class declaration itself, not the body or extends/implements clauses.
     *
     * @param classTree class to check
     * @param type annotated type of the class
     */
    public void checkClass(ClassTree classTree, AnnotatedDeclaredType type) {
        if (!hasDependentAnnotations()) {
            return;
        }

        // TODO: check that invalid annotations in type variable bounds are properly
        // formatted. They are part of the type, but the output isn't nicely formatted.
        checkType(type, classTree);
    }

    /**
     * Checks all Java expressions in the method declaration AnnotatedTypeMirror to see if the
     * expression string is an error string as specified by DependentTypesError#isExpressionError.
     * If the annotated type has any errors, a flowexpr.parse.error is issued.
     *
     * @param methodDeclTree method to check
     * @param type annotated type of the method
     */
    public void checkMethod(MethodTree methodDeclTree, AnnotatedExecutableType type) {
        if (!hasDependentAnnotations()) {
            return;
        }

        // Parameters and receivers are checked by visitVariable
        // So only type parameters and return type need to be checked here.
        checkTypeVariables(methodDeclTree, type);

        // Check return type
        if (type.getReturnType().getKind() != TypeKind.VOID) {
            AnnotatedTypeMirror returnType = factory.getMethodReturnType(methodDeclTree);
            Tree treeForError =
                    TreeUtils.isConstructor(methodDeclTree)
                            ? methodDeclTree
                            : methodDeclTree.getReturnType();
            checkType(returnType, treeForError);
        }
    }

    /**
     * Checks all Java expressions in the type variables to see if the expression string is an error
     * string as specified by DependentTypesError#isExpressionError. If the annotated type has any
     * errors, a flowexpr.parse.error is issued.
     *
     * @param node a method declaration
     * @param methodType annotated type of the method
     */
    private void checkTypeVariables(MethodTree node, AnnotatedExecutableType methodType) {
        Element ele = TreeUtils.elementFromDeclaration(node);
        TypeMirror enclosingType = ElementUtils.enclosingTypeElement(ele).asType();

        JavaExpressionContext context =
                JavaExpressionContext.buildContextForMethodDeclaration(
                        node, enclosingType, factory.getChecker());
        TreePath methodDeclPath = factory.getPath(node);
        for (int i = 0; i < methodType.getTypeVariables().size(); i++) {
            AnnotatedTypeMirror atm = methodType.getTypeVariables().get(i);
            standardizeDoNotUseLocalScope(context, methodDeclPath, atm);
            checkType(atm, node.getTypeParameters().get(i));
        }
    }

    /**
     * Returns true if {@code am} is an expression annotation, that is an annotation whose value is
     * a Java expression.
     *
     * @param am an annotation
     * @return true if {@code am} is an expression annotation
     */
    private boolean isExpressionAnno(AnnotationMirror am) {
        if (!hasDependentAnnotations()) {
            return false;
        }
        for (Class<? extends Annotation> clazz : annoToElements.keySet()) {
            if (factory.areSameByClass(am, clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks all dependent type annotations in the given annotated type to see if the expression
     * string is an error string as specified by DependentTypesError#isExpressionError. If the
     * annotated type has any errors, then a non-empty list of {@link DependentTypesError} is
     * returned.
     */
    private class ExpressionErrorChecker
            extends SimpleAnnotatedTypeScanner<List<DependentTypesError>, Void> {

        /** Create ExpressionErrorChecker. */
        private ExpressionErrorChecker() {
            super(
                    (AnnotatedTypeMirror type, Void aVoid) -> {
                        List<DependentTypesError> errors = new ArrayList<>();
                        for (AnnotationMirror am : type.getAnnotations()) {
                            if (isExpressionAnno(am)) {
                                errors.addAll(errorElements(am));
                            }
                        }
                        return errors;
                    },
                    (r1, r2) -> {
                        List<DependentTypesError> newList = new ArrayList<>(r1);
                        newList.addAll(r2);
                        return newList;
                    },
                    Collections.emptyList());
        }
    }

    /**
     * Copies annotations that might have been viewpoint adapted from the visited type (the first
     * formal parameter) to the second formal parameter.
     */
    private class ViewpointAdaptedCopier extends AnnotatedTypeComparer<Void> {
        @Override
        protected Void scan(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
            if (type == null || p == null) {
                return null;
            }
            Set<AnnotationMirror> replacement = AnnotationUtils.createAnnotationSet();
            for (Class<? extends Annotation> vpa : annoToElements.keySet()) {
                AnnotationMirror anno = type.getAnnotation(vpa);
                if (anno != null) {
                    // Only replace annotations that might have been changed.
                    replacement.add(anno);
                }
            }
            p.replaceAnnotations(replacement);
            if (type.getKind() != p.getKind()) {
                // if the underlying types don't match, then this type has be substituted for a
                // type variable, so don't recur. The primary annotation was copied because
                // if the type variable might have had a primary annotation at a use.
                // For example:
                // <T> void method(@KeyFor("a") T t) {...}
                // void use(@KeyFor("b") String s) {
                //      method(s);  // the type of the parameter should be @KeyFor("a") String
                // }
                return null;
            }
            return super.scan(type, p);
        }

        @Override
        protected Void compare(AnnotatedTypeMirror type, AnnotatedTypeMirror p) {
            if (type == null || p == null) {
                return null;
            }
            if (type.getKind() != p.getKind()) {
                throw new BugInCF("Should be the same. type: %s p: %s ", type, p);
            }
            return null;
        }

        @Override
        protected Void combineRs(Void r1, Void r2) {
            return null;
        }
    }

    /**
     * Return true if {@code atm} has any dependent type annotations. If an annotated type does not
     * have a dependent type annotation, then no standardization or viewpoint adaption is performed.
     * (This check avoids calling time intensive methods unless required.)
     *
     * @param atm a type
     * @return true if {@code atm} has any dependent type annotations
     */
    private boolean hasDependentType(AnnotatedTypeMirror atm) {
        if (atm == null) {
            return false;
        }
        if (!hasDependentAnnotations()) {
            return false;
        }
        boolean b =
                new SimpleAnnotatedTypeScanner<>(
                                (type, p) ->
                                        type.getAnnotations().stream()
                                                .anyMatch(this::isExpressionAnno),
                                Boolean::logicalOr,
                                false)
                        .visit(atm);
        return b;
    }

    /**
     * Returns the elements of the annotation that are Java expressions.
     *
     * @param am AnnotationMirror
     * @return the elements of the annotation that are Java expressions
     */
    private List<String> getListOfExpressionElements(AnnotationMirror am) {
        for (Class<? extends Annotation> clazz : annoToElements.keySet()) {
            if (factory.areSameByClass(am, clazz)) {
                return annoToElements.get(clazz);
            }
        }
        return Collections.emptyList();
    }
}
