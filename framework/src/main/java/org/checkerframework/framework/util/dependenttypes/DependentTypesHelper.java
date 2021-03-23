package org.checkerframework.framework.util.dependenttypes;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
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
import java.util.function.Function;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.expression.FormalParameter;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionConverter;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.visitor.AnnotatedTypeComparer;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * A class that helps checkers use qualifiers that are represented by annotations with Java
 * expression strings. This class performs three main functions:
 *
 * <ol>
 *   <li>Viewpoint-adapts an {@link AnnotationMirror} {@code am}, creating a new one whose Java
 *       expression elements are viewpoint-adapted versions of {@code am}'s. See {@link
 *       #convertAnnotationMirror(StringToJavaExpression, AnnotationMirror)}. Subclasses can
 *       specialize this process by overriding methods in this class.
 *   <li>Changes invalid expression strings to an error string that includes the reason why the
 *       expression is invalid. For example, {@code @KeyFor("m")} would be changed to
 *       {@code @KeyFor("[error for expression: m error: m: identifier not found]")} if m is not a
 *       valid identifier. This allows subtyping checks to assume that if two strings are equal and
 *       not errors, they reference the same valid Java expression.
 *   <li>Checks annotated types for error strings that have been added by this class and issues an
 *       error if any are found.
 * </ol>
 *
 * <p>Steps 2 and 3 are separated so that an error is issued only once per invalid expression string
 * rather than every time the expression string is parsed. (The expression string is parsed multiple
 * times because annotated types are created multiple times.)
 */
public class DependentTypesHelper {

    /** AnnotatedTypeFactory */
    protected final AnnotatedTypeFactory factory;

    // TODO: Using strings is inefficient.  This should probably map to ExecutableElement instead.
    // Also, the key should be annotation names rather than classes.
    /** Maps from an annotation class to the names of its elements that are Java expressions. */
    private final Map<Class<? extends Annotation>, List<String>> annoToElements;

    /** This scans an annotated type and returns a list of {@link DependentTypesError}. */
    private final ExpressionErrorCollector expressionErrorCollector =
            new ExpressionErrorCollector();

    /**
     * A scanner that applies a function to each {@link AnnotationMirror} and replaces it in the
     * given {@code AnnotatedTypeMirror}. (This side-effects the {@code AnnotatedTypeMirror}.)
     */
    private final AnnotatedTypeReplacer annotatedTypeReplacer = new AnnotatedTypeReplacer();

    /**
     * Copies annotations that might have been viewpoint adapted from the visited type (the first
     * formal parameter of {@code ViewpointAdaptedCopier#visit}) to the second formal parameter.
     */
    private final ViewpointAdaptedCopier viewpointAdaptedCopier = new ViewpointAdaptedCopier();

    /** Returns true if the passed AnnotatedTypeMirror has any dependent type annotations. */
    private final AnnotatedTypeScanner<Boolean, Void> hasDependentTypeScanner =
            new SimpleAnnotatedTypeScanner<>(
                    (type, unused) -> {
                        for (AnnotationMirror annotationMirror : type.getAnnotations()) {
                            if (isExpressionAnno(annotationMirror)) {
                                return true;
                            }
                        }
                        return false;
                    },
                    Boolean::logicalOr,
                    false);

    /** The type mirror for java.lang.Object. */
    TypeMirror objectTM;

    /**
     * Creates a {@code DependentTypesHelper}.
     *
     * @param factory annotated type factory
     */
    public DependentTypesHelper(AnnotatedTypeFactory factory) {
        this.factory = factory;

        this.annoToElements = new HashMap<>();
        for (Class<? extends Annotation> expressionAnno : factory.getSupportedTypeQualifiers()) {
            List<String> elementList = getExpressionElementNames(expressionAnno);
            if (!elementList.isEmpty()) {
                annoToElements.put(expressionAnno, elementList);
            }
        }

        this.objectTM =
                TypesUtils.typeFromClass(Object.class, factory.types, factory.getElementUtils());
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
            org.checkerframework.framework.qual.JavaExpression javaExpressionAnno =
                    method.getAnnotation(org.checkerframework.framework.qual.JavaExpression.class);
            if (javaExpressionAnno != null) {
                elements.add(method.getName());
            }
        }
        return elements;
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

    /**
     * Creates a TreeAnnotator that viewpoint-adapts dependent type annotations.
     *
     * @return a new TreeAnnotator that viewpoint-adapts dependent type annotations
     */
    public TreeAnnotator createDependentTypesTreeAnnotator() {
        assert hasDependentAnnotations();
        return new DependentTypesTreeAnnotator(factory, this);
    }

    /**
     * Viewpoint-adapts the dependent type annotations on the bounds of the type parameters of a
     * type to {@code typeUse}.
     *
     * @param typeUse a use of the type with type parameter bounds {@code bounds}
     * @param bounds annotated types of the bounds of the type parameters; its elements are
     *     side-effected by this method (but the list itself is not side-effected)
     */
    public void atParameterizedTypeUse(
            TypeElement typeUse, List<AnnotatedTypeParameterBounds> bounds) {
        if (!hasDependentAnnotations()) {
            return;
        }

        StringToJavaExpression stringToJavaExpr =
                stringExpr ->
                        StringToJavaExpression.atTypeDecl(
                                stringExpr, typeUse, factory.getChecker());
        for (AnnotatedTypeParameterBounds bound : bounds) {
            convertAnnotatedTypeMirror(stringToJavaExpr, bound.getUpperBound());
            convertAnnotatedTypeMirror(stringToJavaExpr, bound.getLowerBound());
        }
    }

    /**
     * Viewpoint-adapts the dependent type annotations in the methodType to the
     * methodInvocationTree.
     *
     * <p>{@code methodType} has been viewpoint-adapted to the call site, except for any dependent
     * type annotations. This method viewpoint-adapts the dependent type annotations.
     *
     * @param methodInvocationTree use of the method
     * @param methodType type of the method invocation; is side-effected by this method
     */
    public void atMethodInvocation(
            MethodInvocationTree methodInvocationTree, AnnotatedExecutableType methodType) {
        if (!hasDependentAnnotations()) {
            return;
        }
        atInvocation(methodInvocationTree, methodType);
    }

    /**
     * Viewpoint-adapts the dependent type annotations in the constructorType to the newClassTree.
     *
     * <p>{@code constructorType} has been viewpoint-adapted to the call site, except for any
     * dependent type annotations. This method viewpoint-adapts the dependent type annotations.
     *
     * @param newClassTree invocation of the constructor
     * @param constructorType type of the constructor invocation; is side-effected by this method
     */
    public void atConstructorInvocation(
            NewClassTree newClassTree, AnnotatedExecutableType constructorType) {
        if (!hasDependentAnnotations()) {
            return;
        }
        atInvocation(newClassTree, constructorType);
    }

    /**
     * Viewpoint-adapts a method or constructor invocation.
     *
     * <p>{@code methodType} has been viewpoint-adapted to the call site, except for any dependent
     * type annotations. This method viewpoint-adapts the dependent type annotations.
     *
     * @param tree invocation of the method or constructor
     * @param methodType type of the method or constructor invocation; is side-effected by this
     *     method
     */
    private void atInvocation(ExpressionTree tree, AnnotatedExecutableType methodType) {
        assert hasDependentAnnotations();
        Element methodElt = TreeUtils.elementFromUse(tree);
        // Because methodType is the type post type variable substitution, it has annotations from
        // both the method declaration and the type arguments at the use of the method. Annotations
        // from type arguments must not be viewpoint-adapted to the call site. For example:
        //   Map<String, String> map = ...;
        //   List<@KeyFor("this.map") String> list = ...;
        //   list.get(0)
        //
        // methodType is @KeyFor("this.map") String get(int)
        // "this.map" must not be viewpoint-adapted to the invocation because it is not from
        // the method declaration, but added during type variable substitution.
        //
        // So this implementation gets the declared type of the method, declaredMethodType,
        // viewpoint-adapts all dependent type annotations in declaredMethodType to the call site,
        // and then copies the viewpoint-adapted annotations from methodType except for types that
        // are replace by type variable substitution.

        // The annotations on `declaredMethodType` will be copied to `methodType`.
        AnnotatedExecutableType declaredMethodType =
                (AnnotatedExecutableType) factory.getAnnotatedType(methodElt);
        if (!hasDependentType(declaredMethodType)) {
            return;
        }

        StringToJavaExpression stringToJavaExpr;
        if (tree instanceof MethodInvocationTree) {
            stringToJavaExpr =
                    stringExpr ->
                            StringToJavaExpression.atMethodInvocation(
                                    stringExpr, (MethodInvocationTree) tree, factory.getChecker());
        } else if (tree instanceof NewClassTree) {
            stringToJavaExpr =
                    stringExpr ->
                            StringToJavaExpression.atConstructorInvocation(
                                    stringExpr, (NewClassTree) tree, factory.getChecker());
        } else {
            throw new BugInCF("Unexpected tree: %s kind: %s", tree, tree.getKind());
        }
        convertAnnotatedTypeMirror(stringToJavaExpr, declaredMethodType);
        this.viewpointAdaptedCopier.visit(declaredMethodType, methodType);
    }

    /**
     * Viewpoint-adapts the Java expressions in annotations written on a field declaration to the
     * use at {@code fieldAccess}.
     *
     * @param fieldAccess a field access
     * @param type its type; is side-effected by this method
     */
    public void atFieldAccess(MemberSelectTree fieldAccess, AnnotatedTypeMirror type) {
        if (!hasDependentType(type)) {
            return;
        }

        convertAnnotatedTypeMirror(
                stringExpr ->
                        StringToJavaExpression.atFieldAccess(
                                stringExpr, fieldAccess, factory.getChecker()),
                type);
    }

    /**
     * Standardizes the Java expressions in annotations to a class declaration.
     *
     * @param type the type of the type declaration; is side-effected by this method
     * @param typeElt the element of the type declaration
     */
    public void atTypeDecl(AnnotatedTypeMirror type, TypeElement typeElt) {
        if (!hasDependentType(type)) {
            return;
        }

        convertAnnotatedTypeMirror(
                stringExpr ->
                        StringToJavaExpression.atTypeDecl(
                                stringExpr, typeElt, factory.getChecker()),
                type);
    }

    /**
     * Standardizes the Java expressions in annotations to the method declaration.
     *
     * @param methodDeclTree a method declaration
     * @param atm the method return type; is side-effected by this method
     */
    public void atReturnType(MethodTree methodDeclTree, AnnotatedTypeMirror atm) {
        if (!hasDependentType(atm)) {
            return;
        }

        convertAnnotatedTypeMirror(
                stringExpr ->
                        StringToJavaExpression.atMethodBody(
                                stringExpr, methodDeclTree, factory.getChecker()),
                atm);
    }

    /** A set containing {@link Tree.Kind#METHOD} and {@link Tree.Kind#LAMBDA_EXPRESSION}. */
    private static final Set<Tree.Kind> METHOD_OR_LAMBDA =
            EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);

    /**
     * Standardize the Java expressions in annotations in a variable declaration.
     *
     * @param declarationTree the variable declaration
     * @param type the type of the variable declaration; is side-effected by this method
     * @param variableElt the element of the variable declaration
     */
    public void atVariableDeclaration(
            Tree declarationTree, AnnotatedTypeMirror type, VariableElement variableElt) {
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
                    throw new BugInCF("no enclosing method or lambda found for " + variableElt);
                }
                Tree enclTree = pathTillEnclTree.getLeaf();

                if (enclTree.getKind() == Kind.METHOD) {
                    MethodTree methodDeclTree = (MethodTree) enclTree;
                    convertAnnotatedTypeMirror(
                            stringExpr ->
                                    StringToJavaExpression.atMethodBody(
                                            stringExpr, methodDeclTree, factory.getChecker()),
                            type);
                } else {
                    // Lambdas can use local variables defined in the enclosing method, so allow
                    // identifiers to be locals in scope at the location of the lambda.
                    convertAnnotatedTypeMirror(
                            stringExpr ->
                                    StringToJavaExpression.atLambdaParameter(
                                            stringExpr,
                                            (LambdaExpressionTree) enclTree,
                                            pathToVariableDecl.getParentPath(),
                                            factory.getChecker()),
                            type);
                }
                break;

            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                convertAnnotatedTypeMirror(
                        stringExpr ->
                                StringToJavaExpression.atPath(
                                        stringExpr, pathToVariableDecl, factory.getChecker()),
                        type);
                break;

            case FIELD:
            case ENUM_CONSTANT:
                convertAnnotatedTypeMirror(
                        stringExpr ->
                                StringToJavaExpression.atFieldDecl(
                                        stringExpr, variableElt, factory.getChecker()),
                        type);
                break;

            default:
                throw new BugInCF(
                        "unexpected element kind " + variableElt.getKind() + " for " + variableElt);
        }
    }

    /**
     * Standardize the Java expressions in annotations in the type of an expression.
     *
     * @param tree an expression
     * @param annotatedType its type; is side-effected by this method
     */
    public void atExpression(ExpressionTree tree, AnnotatedTypeMirror annotatedType) {
        if (!hasDependentType(annotatedType)) {
            return;
        }

        TreePath path = factory.getPath(tree);
        if (path == null) {
            return;
        }
        convertAnnotatedTypeMirror(
                stringExpr -> StringToJavaExpression.atPath(stringExpr, path, factory.getChecker()),
                annotatedType);
    }

    /**
     * Standardize the Java expressions in annotations in a type.
     *
     * @param type the type to standardize; is side-effected by this method
     * @param elt the element whose type is {@code type}
     */
    public void atLocalVariable(AnnotatedTypeMirror type, Element elt) {
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

                atVariableDeclaration(declarationTree, type, (VariableElement) elt);
                return;

            default:
                // It's not a local variable (it might be METHOD, CONSTRUCTOR, CLASS, or INTERFACE,
                // for example), so there is nothing to do.
                break;
        }
    }

    /**
     * Viewpoint-adapt all dependent type annotations to the method declaration, {@code
     * methodDeclTree}. This method changes occurrences of formal parameter names to the "#2"
     * syntax, and it removes expressions that contain other local variables.
     *
     * <p>If a Java expression in {@code atm} references local variables (other than formal
     * parameters), the expression is removed from the annotation. This could result in dependent
     * type annotations with empty lists of expressions. If this is a problem, subclasses can
     * override {@link #buildAnnotation(AnnotationMirror, Map)} to do something besides creating an
     * annotation with a empty list.
     *
     * @param methodDeclTree the method declaration to which the annotations are viewpoint-adapted
     * @param atm type to viewpoint-adapt; is side-effected by this method
     */
    public void delocalize(MethodTree methodDeclTree, AnnotatedTypeMirror atm) {
        if (!hasDependentType(atm)) {
            return;
        }

        TreePath pathToMethodDecl = factory.getPath(methodDeclTree);
        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(methodDeclTree);
        List<FormalParameter> parameters = JavaExpression.getFormalParameters(methodElement);
        List<JavaExpression> paramsAsLocals =
                JavaExpression.getParametersAsLocalVariables(methodElement);

        /** Thrown when a non-parameter local variable is found. */
        @SuppressWarnings("serial")
        class FoundLocalException extends RuntimeException {}

        StringToJavaExpression stringToJavaExpr =
                expression -> {
                    JavaExpression result;
                    try {
                        result =
                                StringToJavaExpression.atPath(
                                        expression, pathToMethodDecl, factory.getChecker());
                    } catch (JavaExpressionParseException ex) {
                        return null;
                    }
                    JavaExpressionConverter jec =
                            new JavaExpressionConverter() {
                                @Override
                                protected JavaExpression visitLocalVariable(
                                        LocalVariable localVarExpr, Void unused) {
                                    int index = paramsAsLocals.indexOf(localVarExpr);
                                    if (index == -1) {
                                        throw new FoundLocalException();
                                    }
                                    return parameters.get(index);
                                }
                            };
                    try {
                        return jec.convert(result);
                    } catch (FoundLocalException ex) {
                        return null;
                    }
                };
        convertAnnotatedTypeMirror(stringToJavaExpr, atm);
    }

    /**
     * Calls {@link #convertAnnotationMirror(StringToJavaExpression, AnnotationMirror)} on each
     * annotation mirror on type with {@code stringToJavaExpr}. And replaces the annotation with the
     * one created by {@code convertAnnotationMirror}, if it's not null. If it is null, the original
     * annotation is used. See {@link #convertAnnotationMirror(StringToJavaExpression,
     * AnnotationMirror)} for more details.
     *
     * @param stringToJavaExpr function to convert a string to a {@link JavaExpression}
     * @param type the type that is side-effected by this method
     */
    protected void convertAnnotatedTypeMirror(
            StringToJavaExpression stringToJavaExpr, AnnotatedTypeMirror type) {
        this.annotatedTypeReplacer.visit(
                type, anno -> convertAnnotationMirror(stringToJavaExpr, anno));
    }

    /**
     * Given an annotation {@code anno}, this method builds a new annotation with the Java
     * expressions transformed according to {@code stringToJavaExpr}. If {@code anno} is not a
     * dependent type annotation, {@code null} is returned.
     *
     * <p>If {@code stringToJavaExpr} returns {@code null}, then that expression is removed from the
     * returned annotation.
     *
     * <p>Instead of overriding this method, subclasses can override the following methods to change
     * the behavior of this class:
     *
     * <ul>
     *   <li>{@link #shouldParseExpression(String)}: to control which expressions are skipped. If
     *       this method returns false, then the expression string is not parsed and is included in
     *       the new annotation unchanged.
     *   <li>{@link #transform(JavaExpression)}: make changes to the JavaExpression produced by
     *       {@code stringToJavaExpr}.
     *   <li>{@link #buildAnnotation(AnnotationMirror, Map)}: to change the annotation returned by
     *       this method.
     * </ul>
     *
     * @param stringToJavaExpr function that converts strings to {@code JavaExpression}s
     * @param anno annotation mirror
     * @return an annotation created by applying {@code stringToJavaExpr} to all expression strings
     *     in {@code anno}
     */
    public @Nullable AnnotationMirror convertAnnotationMirror(
            StringToJavaExpression stringToJavaExpr, AnnotationMirror anno) {
        if (!isExpressionAnno(anno)) {
            return null;
        }

        Map<String, List<JavaExpression>> newElements = new HashMap<>();
        for (String value : getListOfExpressionElements(anno)) {
            List<String> expressionStrings =
                    AnnotationUtils.getElementValueArray(anno, value, String.class, true);
            List<JavaExpression> javaExprs = new ArrayList<>(expressionStrings.size());
            newElements.put(value, javaExprs);
            for (String expression : expressionStrings) {
                JavaExpression result;
                if (shouldParseExpression(expression)) {
                    try {
                        result = stringToJavaExpr.toJavaExpression(expression);
                    } catch (JavaExpressionParseException e) {
                        result = createError(expression, e);
                    }
                } else {
                    result = createPassThroughString(expression);
                }

                if (result != null) {
                    result = transform(result);
                    javaExprs.add(result);
                }
            }
        }
        return buildAnnotation(anno, newElements);
    }

    /**
     * This method is for subclasses to override to change JavaExpressions in some way before they
     * are inserted into new annotations. This method is called after parsing and
     * viewpoint-adaptation have occurred. {@code javaExpr} may be a {@link PassThroughExpression}
     * that results from a {@link JavaExpressionParseException}.
     *
     * <p>If {@code null} is returned then the expression is not added to the new annotation.
     *
     * <p>This implementation returns the argument.
     *
     * @param javaExpr a JavaExpression
     * @return a transformed JavaExpression or {@code null} if no transformation exists
     */
    protected @Nullable JavaExpression transform(JavaExpression javaExpr) {
        return javaExpr;
    }

    /**
     * Whether or not {@code expression} should be parsed. The default implementation returns false
     * if the {@code expression} is an expression error according to {@link
     * DependentTypesError#isExpressionError(String)}. Subclasses may override this method to add
     * additional logic.
     *
     * <p>If this method returns false, the {@code expression} is not parsed and will appear in the
     * dependent types annotation as is.
     *
     * @param expression an expression string in a dependent types annotation
     * @return whether or not {@code expression} should be parsed
     */
    protected boolean shouldParseExpression(String expression) {
        return !DependentTypesError.isExpressionError(expression);
    }

    /**
     * Create a new annotation of the same type as {@code originalAnno} using the provided {@code
     * elementMap}.
     *
     * @param originalAnno the annotation passed to {@link
     *     #convertAnnotationMirror(StringToJavaExpression, AnnotationMirror)}
     * @param elementMap a mapping of element names of {@code originalAnno} to {@code
     *     JavaExpression}s
     * @return an annotation created from {@code elementMap}
     */
    protected AnnotationMirror buildAnnotation(
            AnnotationMirror originalAnno, Map<String, List<JavaExpression>> elementMap) {
        AnnotationBuilder builder =
                new AnnotationBuilder(
                        factory.getProcessingEnv(), AnnotationUtils.annotationName(originalAnno));

        for (Map.Entry<String, List<JavaExpression>> entry : elementMap.entrySet()) {
            String value = entry.getKey();
            List<String> strings = SystemUtil.mapList(JavaExpression::toString, entry.getValue());
            builder.setValue(value, strings);
        }
        return builder.build();
    }

    /**
     * A {@link JavaExpression} that does not represent a {@link JavaExpression}, but rather allows
     * an expression string to be converted to a JavaExpression and then to a string without
     * parsing.
     */
    static class PassThroughExpression extends Unknown {
        /** Some string. */
        public final String string;

        /**
         * Creates a PassThroughExpression.
         *
         * @param type some type
         * @param string the string to convert to a JavaExpression
         */
        public PassThroughExpression(TypeMirror type, String string) {
            super(type);
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    /**
     * Allows an expression string to be converted to a JavaExpression and then to a string without
     * parsing the string.
     *
     * @param string some string
     * @return a {@link PassThroughExpression}
     */
    protected PassThroughExpression createPassThroughString(String string) {
        return new PassThroughExpression(objectTM, string);
    }

    /**
     * Creates a {@link JavaExpression} representing the exception thrown when parsing {@code
     * expression}.
     *
     * @param expression an expression that cause {@code e} when parsed
     * @param e the exception thrown when parsing {@code expression}
     * @return a java expression
     */
    protected PassThroughExpression createError(String expression, JavaExpressionParseException e) {
        return new PassThroughExpression(
                objectTM, new DependentTypesError(expression, e).toString());
    }

    /**
     * Creates a {@link JavaExpression} representing the error caused when parsing {@code
     * expression}
     *
     * @param expression an expression that cause {@code e} when parsed
     * @param error the error message caused by {@code expression}
     * @return a java expression
     */
    protected PassThroughExpression createError(String expression, String error) {
        return new PassThroughExpression(
                objectTM, new DependentTypesError(expression, error).toString());
    }

    /**
     * Applies the passed function to each annotation in the given {@link AnnotatedTypeMirror}. If
     * the function returns a non-null annotation, then the original annotation is replaced with the
     * result. If the function returns null, the original annotation is retained.
     */
    private static class AnnotatedTypeReplacer
            extends AnnotatedTypeScanner<Void, Function<AnnotationMirror, AnnotationMirror>> {

        @Override
        public Void visitTypeVariable(
                AnnotatedTypeMirror.AnnotatedTypeVariable type,
                Function<AnnotationMirror, AnnotationMirror> func) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, null);

            // If the type variable has a primary annotation, then it is viewpoint-adapted before
            // this method is called.  The viewpoint-adapted primary annotation was already copied
            // to the upper and lower bounds.  These annotations cannot be viewpoint-adapted again,
            // so remove them, viewpoint-adapt any other annotations in the bound, and then add them
            // back.
            Set<AnnotationMirror> primarys = type.getAnnotations();
            type.getLowerBound().removeAnnotations(primarys);
            Void r = scan(type.getLowerBound(), func);
            type.getLowerBound().addAnnotations(primarys);
            visitedNodes.put(type, r);

            type.getUpperBound().removeAnnotations(primarys);
            r = scanAndReduce(type.getUpperBound(), func, r);
            type.getUpperBound().addAnnotations(primarys);
            visitedNodes.put(type, r);
            return r;
        }

        @Override
        protected Void scan(
                AnnotatedTypeMirror type, Function<AnnotationMirror, AnnotationMirror> func) {
            for (AnnotationMirror anno :
                    AnnotationUtils.createAnnotationSet(type.getAnnotations())) {
                AnnotationMirror newAnno = func.apply(anno);
                if (newAnno != null) {
                    // This code must remove and then add, rather then call replace, because type
                    // may have multiple annotations with the same class, but different elements.
                    // (This is a bug; see
                    // https://github.com/typetools/checker-framework/issues/4451.)
                    // AnnotatedTypeMirror#replace only removes one annotation that is in the same
                    // hierarchy as the passed argument.
                    type.removeAnnotation(anno);
                    type.addAnnotation(newAnno);
                }
            }
            return super.scan(type, func);
        }
    }

    /**
     * Reports an expression.unparsable.type.invalid error for each Java expression in the given
     * type that is an expression error string.
     *
     * @param atm annotated type to check for expression errors
     * @param errorTree the tree at which to report any found errors
     */
    public void checkTypeForErrorExpressions(AnnotatedTypeMirror atm, Tree errorTree) {
        if (!hasDependentAnnotations()) {
            return;
        }

        List<DependentTypesError> errors = expressionErrorCollector.visit(atm);
        if (errors == null || errors.isEmpty()) {
            return;
        }

        if (errorTree.getKind() == Kind.VARIABLE) {
            ModifiersTree modifiers = ((VariableTree) errorTree).getModifiers();
            errorTree = ((VariableTree) errorTree).getType();
            for (AnnotationTree annoTree : modifiers.getAnnotations()) {
                String annoString = annoTree.toString();
                for (Class<?> annoClazz : annoToElements.keySet()) {
                    // TODO: Simple string containment seems too simplistic.  At least check for a
                    // word boundary.
                    if (annoString.contains(annoClazz.getSimpleName())) {
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
     * Returns a list of {@link DependentTypesError}s for all the Java expression elements of the
     * annotation that are an error string as specified by DependentTypesError#isExpressionError.
     *
     * @param am an annotation
     * @return the list {@link DependentTypesError}s
     */
    private List<DependentTypesError> errorElements(AnnotationMirror am) {
        assert hasDependentAnnotations();

        List<DependentTypesError> errors = new ArrayList<>();

        for (String element : getListOfExpressionElements(am)) {
            // It's always an array, not a single value, because @JavaExpression may only be written
            // on an annotation element of type String[].
            List<String> value =
                    AnnotationUtils.getElementValueArray(am, element, String.class, false);
            for (String v : value) {
                if (DependentTypesError.isExpressionError(v)) {
                    errors.add(DependentTypesError.unparse(v));
                }
            }
        }
        return errors;
    }

    /**
     * Reports a flowexpr.parse.error error for each Java expression in the given annotation that is
     * an expression error string.
     *
     * @param annotation annotation to check
     * @param errorTree location at which to issue errors
     */
    public void checkAnnotationForErrorExpressions(AnnotationMirror annotation, Tree errorTree) {
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
     * Reports an expression.unparsable.type.invalid error for each Java expression in the given
     * class declaration AnnotatedTypeMirror that is an expression error string. Note that this
     * reports errors in the class declaration itself, not the body or extends/implements clauses.
     *
     * @param classTree class to check
     * @param type annotated type of the class
     */
    public void checkClassForErrorExpressions(ClassTree classTree, AnnotatedDeclaredType type) {
        if (!hasDependentAnnotations()) {
            return;
        }

        // TODO: check that invalid annotations in type variable bounds are properly
        // formatted. They are part of the type, but the output isn't nicely formatted.
        checkTypeForErrorExpressions(type, classTree);
    }

    /**
     * Reports an expression.unparsable.type.invalid error for each Java expression in the method
     * declaration AnnotatedTypeMirror that is an expression error string.
     *
     * @param methodDeclTree method to check
     * @param type annotated type of the method
     */
    public void checkMethodForErrorExpressions(
            MethodTree methodDeclTree, AnnotatedExecutableType type) {
        if (!hasDependentAnnotations()) {
            return;
        }

        // Parameters and receivers are checked by visitVariable
        // So only type parameters and return type need to be checked here.
        checkTypeVariablesForErrorExpressions(methodDeclTree, type);

        // Check return type
        if (type.getReturnType().getKind() != TypeKind.VOID) {
            AnnotatedTypeMirror returnType = factory.getMethodReturnType(methodDeclTree);
            Tree treeForError =
                    TreeUtils.isConstructor(methodDeclTree)
                            ? methodDeclTree
                            : methodDeclTree.getReturnType();
            checkTypeForErrorExpressions(returnType, treeForError);
        }
    }

    /**
     * Reports an expression.unparsable.type.invalid error for each Java expression in the given
     * type variables that is an expression error string.
     *
     * @param node a method declaration
     * @param methodType annotated type of the method
     */
    private void checkTypeVariablesForErrorExpressions(
            MethodTree node, AnnotatedExecutableType methodType) {
        for (int i = 0; i < methodType.getTypeVariables().size(); i++) {
            AnnotatedTypeMirror atm = methodType.getTypeVariables().get(i);
            convertAnnotatedTypeMirror(
                    stringExpr ->
                            StringToJavaExpression.atMethodBody(
                                    stringExpr, node, factory.getChecker()),
                    atm);
            checkTypeForErrorExpressions(atm, node.getTypeParameters().get(i));
        }
    }

    /**
     * Returns true if {@code am} is an expression annotation, that is, an annotation whose element
     * is a Java expression.
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
    private class ExpressionErrorCollector
            extends SimpleAnnotatedTypeScanner<List<DependentTypesError>, Void> {

        /** Create ExpressionErrorCollector. */
        private ExpressionErrorCollector() {
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
                    DependentTypesHelper::concatenate,
                    Collections.emptyList());
        }
    }

    /**
     * Appends list2 to list1 in a new list. If either list is empty, returns the other. Thus, the
     * result may be aliased to one of the arguments and the client should only read, not write, the
     * result.
     *
     * @param list1 a list
     * @param list2 a list
     * @return the lists, concatenated
     */
    private static List<DependentTypesError> concatenate(
            List<DependentTypesError> list1, List<DependentTypesError> list2) {
        if (list1.isEmpty()) {
            return list2;
        } else if (list2.isEmpty()) {
            return list1;
        }
        List<DependentTypesError> newList = new ArrayList<>(list1.size() + list2.size());
        newList.addAll(list1);
        newList.addAll(list2);
        return newList;
    }

    /**
     * Copies annotations that might have been viewpoint-adapted from the visited type (the first
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
                // If the underlying types don't match, then this type has been substituted for a
                // type variable, so don't recur. The primary annotation was copied because
                // the type variable might have had a primary annotation at a use.
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
     * Returns true if {@code atm} has any dependent type annotations. If an annotated type does not
     * have a dependent type annotation, then no standardization or viewpoint adaption is performed.
     * (This check avoids calling time-intensive methods unless required.)
     *
     * @param atm a type
     * @return true if {@code atm} has any dependent type annotations
     */
    private boolean hasDependentType(AnnotatedTypeMirror atm) {
        if (atm == null) {
            return false;
        }
        // This is a test about the type system.
        if (!hasDependentAnnotations()) {
            return false;
        }
        // This is a test about this specific type.
        return hasDependentTypeScanner.visit(atm);
    }
}
