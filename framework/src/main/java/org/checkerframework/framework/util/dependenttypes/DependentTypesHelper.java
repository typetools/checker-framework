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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.qual.JavaExpression;
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
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.UtilPlume;

/**
 * A class that helps checkers use qualifiers that are represented by annotations with Java
 * expression strings. This class performs four main functions:
 *
 * <ol>
 *   <li>Standardizes/canonicalizes the expressions in the annotations such that two expression
 *       strings that are equivalent are made to be equal. For example, an instance field f may
 *       appear in an expression string as "f" or "this.f"; this class standardizes both strings to
 *       "this.f".
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

    public DependentTypesHelper(AnnotatedTypeFactory factory) {
        this.factory = factory;

        this.annoToElements = new HashMap<>();
        for (Class<? extends Annotation> expressionAnno : factory.getSupportedTypeQualifiers()) {
            List<String> elementList = getExpressionElementNames(expressionAnno);
            if (elementList != null && !elementList.isEmpty()) {
                annoToElements.put(expressionAnno, elementList);
            }
        }
    }

    /**
     * Returns a list of the names of elements in the annotation class that should be interpreted as
     * Java expressions.
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
            JavaExpression javaExpression = method.getAnnotation(JavaExpression.class);
            if (javaExpression != null) {
                elements.add(method.getName());
            }
        }
        return elements;
    }

    /**
     * Creates a TreeAnnotator that standardizes dependent type annotations.
     *
     * @param factory annotated type factory
     * @return a new TreeAnnotator that standardizes dependent type annotations
     */
    public TreeAnnotator createDependentTypesTreeAnnotator(AnnotatedTypeFactory factory) {
        return new DependentTypesTreeAnnotator(factory, this);
    }

    /**
     * Viewpoint-adapts the dependent type annotations on the bounds to the use of the type.
     *
     * @param classDecl class or interface declaration whose type variables should be viewpoint
     *     adapted
     * @param bounds annotated types of the bounds of the type variables
     * @param pathToUse tree path to the use of the class or interface
     */
    public void viewpointAdaptTypeVariableBounds(
            TypeElement classDecl, List<AnnotatedTypeParameterBounds> bounds, TreePath pathToUse) {
        FlowExpressions.Receiver r = FlowExpressions.internalReprOfImplicitReceiver(classDecl);
        FlowExpressionContext context = new FlowExpressionContext(r, null, factory.getContext());
        for (AnnotatedTypeParameterBounds bound : bounds) {
            standardizeDoNotUseLocals(context, pathToUse, bound.getUpperBound());
            standardizeDoNotUseLocals(context, pathToUse, bound.getLowerBound());
        }
    }

    /**
     * Viewpoint-adapts the dependent type annotations in the methodDeclType based on the
     * methodInvocationTree.
     *
     * @param methodInvocationTree use of the method
     * @param methodDeclType type of the method declaration
     */
    public void viewpointAdaptMethod(
            MethodInvocationTree methodInvocationTree, AnnotatedExecutableType methodDeclType) {
        ExpressionTree receiverTree = TreeUtils.getReceiverTree(methodInvocationTree);
        List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
        viewpointAdaptExecutable(methodInvocationTree, receiverTree, methodDeclType, args);
    }

    /**
     * Viewpoint-adapts the dependent type annotations in the constructorType based on the
     * newClassTree.
     *
     * @param newClassTree invocation of the constructor
     * @param constructorType type of the constructor
     */
    public void viewpointAdaptConstructor(
            NewClassTree newClassTree, AnnotatedExecutableType constructorType) {
        ExpressionTree receiverTree = newClassTree.getEnclosingExpression();
        List<? extends ExpressionTree> args = newClassTree.getArguments();
        viewpointAdaptExecutable(newClassTree, receiverTree, constructorType, args);
    }

    private void viewpointAdaptExecutable(
            ExpressionTree tree,
            ExpressionTree receiverTree,
            AnnotatedExecutableType typeFromUse,
            List<? extends ExpressionTree> args) {

        Element element = TreeUtils.elementFromUse(tree);
        AnnotatedExecutableType viewpointAdaptedType =
                (AnnotatedExecutableType) factory.getAnnotatedType(element);
        if (!hasDependentType(viewpointAdaptedType)) {
            return;
        }

        FlowExpressions.Receiver receiver;
        if (receiverTree == null) {
            receiver =
                    FlowExpressions.internalReprOfImplicitReceiver(TreeUtils.elementFromUse(tree));
        } else {
            receiver = FlowExpressions.internalReprOf(factory, receiverTree);
        }

        List<Receiver> argReceivers = new ArrayList<>();
        boolean isVarargs = false;
        if (tree.getKind() == Kind.METHOD_INVOCATION) {
            ExecutableElement methodCalled = TreeUtils.elementFromUse((MethodInvocationTree) tree);
            if (isVarArgsMethodInvocation(methodCalled, typeFromUse, args)) {
                isVarargs = true;
                for (int i = 0; i < methodCalled.getParameters().size() - 1; i++) {
                    argReceivers.add(FlowExpressions.internalReprOf(factory, args.get(i)));
                }
                List<Receiver> varargArgs = new ArrayList<>();
                for (int i = methodCalled.getParameters().size() - 1; i < args.size(); i++) {
                    varargArgs.add(FlowExpressions.internalReprOf(factory, args.get(i)));
                }
                Element varargsElement =
                        methodCalled.getParameters().get(methodCalled.getParameters().size() - 1);
                TypeMirror tm = ElementUtils.getType(varargsElement);
                argReceivers.add(
                        new FlowExpressions.ArrayCreation(tm, Collections.emptyList(), varargArgs));
            }
        }

        if (!isVarargs) {
            for (ExpressionTree argTree : args) {
                argReceivers.add(FlowExpressions.internalReprOf(factory, argTree));
            }
        }

        TreePath currentPath = factory.getPath(tree);

        FlowExpressionContext context =
                new FlowExpressionContext(receiver, argReceivers, factory.getContext());

        // typeForUse cannot be viewpoint adapted directly because it is the type post type variable
        // substitution.  Dependent type annotations on type arguments do not (and cannot) be
        // viewpoint adapted along with the dependent type annotations that are on the method
        // declaration. For example:
        // Map<String, String> map = ...;
        // List<@KeyFor("map") String> list = ...;
        // list.get(0)
        // If the type of List.get is viewpoint adapted for the invocation "list.get(0)", then
        // typeFromUse would be @KeyFor("map") String get(int).
        // Instead, use the type for the method (viewpointAdaptedType) and viewpoint adapt that
        // type.
        // Then copy annotations from the viewpoint adapted type to typeFromUse, if that annotation
        // is not on a type that was substituted for a type variable.

        standardizeDoNotUseLocals(context, currentPath, viewpointAdaptedType);
        new ViewpointAdaptedCopier().visit(viewpointAdaptedType, typeFromUse);
    }

    /**
     * Returns true if methodCalled is varargs method invocation and its varargs arguments are not
     * passed in an array, false otherwise.
     *
     * @return true if methodCalled is varargs method invocation and its varargs arguments are not
     *     passed in an array, false otherwise
     */
    private boolean isVarArgsMethodInvocation(
            ExecutableElement methodCalled,
            AnnotatedExecutableType typeFromUse,
            List<? extends ExpressionTree> args) {
        if (methodCalled != null && methodCalled.isVarArgs()) {
            if (methodCalled.getParameters().size() == args.size()) {
                AnnotatedTypeMirror lastArg = factory.getAnnotatedType(args.get(args.size() - 1));
                List<AnnotatedTypeMirror> argTypes = typeFromUse.getParameterTypes();
                AnnotatedArrayType varargsArg =
                        (AnnotatedArrayType) argTypes.get(argTypes.size() - 1);
                return lastArg.getKind() != TypeKind.ARRAY
                        || AnnotatedTypes.getArrayDepth(varargsArg)
                                != AnnotatedTypes.getArrayDepth((AnnotatedArrayType) lastArg);
            }
            return true;
        }
        return false;
    }

    /**
     * Standardizes new class declarations in Java expressions.
     *
     * @param tree the new class declaration
     * @param type the type representing the class
     */
    public void standardizeNewClassTree(NewClassTree tree, AnnotatedDeclaredType type) {
        if (!hasDependentType(type)) {
            return;
        }

        TreePath path = factory.getPath(tree);
        Tree enclosingClass = TreeUtils.enclosingClass(path);
        if (enclosingClass == null) {
            return;
        }
        TypeMirror enclosingType = TreeUtils.typeOf(enclosingClass);
        FlowExpressions.Receiver r =
                FlowExpressions.internalReprOfPseudoReceiver(path, enclosingType);
        FlowExpressionContext context =
                new FlowExpressionContext(
                        r,
                        FlowExpressions.getParametersOfEnclosingMethod(factory, path),
                        factory.getContext());
        standardizeUseLocals(context, path, type);
    }

    /**
     * Standardizes a method return in a Java expression.
     *
     * @param m the method to be standardized
     * @param atm the method return type
     */
    public void standardizeReturnType(MethodTree m, AnnotatedTypeMirror atm) {
        if (atm.getKind() == TypeKind.NONE) {
            return;
        }
        if (!hasDependentType(atm)) {
            return;
        }

        Element ele = TreeUtils.elementFromDeclaration(m);
        TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();

        FlowExpressionContext context =
                FlowExpressionContext.buildContextForMethodDeclaration(
                        m, enclosingType, factory.getContext());
        standardizeDoNotUseLocals(context, factory.getPath(m), atm);
    }

    /**
     * Standardize the Java expressions in annotations in a class declaration.
     *
     * @param node the class declaration
     * @param type the type of the class declaration
     * @param ele the element of the class declaration
     */
    public void standardizeClass(ClassTree node, AnnotatedTypeMirror type, Element ele) {
        if (!hasDependentType(type)) {
            return;
        }
        TreePath path = factory.getPath(node);
        if (path == null) {
            return;
        }
        FlowExpressions.Receiver receiverF = FlowExpressions.internalReprOfImplicitReceiver(ele);
        FlowExpressionContext classContext =
                new FlowExpressionContext(receiverF, null, factory.getContext());
        standardizeDoNotUseLocals(classContext, path, type);
    }

    /**
     * Standardize the Java expressions in annotations in a variable declaration.
     *
     * @param node the variable declaration
     * @param type the type of the variable declaration
     * @param ele the element of the variable declaration
     */
    public void standardizeVariable(Tree node, AnnotatedTypeMirror type, Element ele) {
        if (!hasDependentType(type)) {
            return;
        }

        TreePath path = factory.getPath(node);
        if (path == null) {
            return;
        }
        switch (ele.getKind()) {
            case PARAMETER:
                Tree enclTree =
                        TreeUtils.enclosingOfKind(
                                path,
                                new HashSet<>(Arrays.asList(Kind.METHOD, Kind.LAMBDA_EXPRESSION)));

                if (enclTree.getKind() == Kind.METHOD) {
                    // If the most enclosing tree is a method, the parameter is a method parameter
                    MethodTree methodTree = (MethodTree) enclTree;
                    TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();
                    FlowExpressionContext parameterContext =
                            FlowExpressionContext.buildContextForMethodDeclaration(
                                    methodTree, enclosingType, factory.getContext());
                    standardizeDoNotUseLocals(parameterContext, path, type);
                } else {
                    // Otherwise, the parameter is a lambda parameter
                    LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclTree;
                    FlowExpressionContext parameterContext =
                            FlowExpressionContext.buildContextForLambda(
                                    lambdaTree, path, factory.getContext());
                    // TODO: test this.
                    // TODO: use path.getParentPath to prevent a StackOverflowError, see Issue
                    // #1027.
                    standardizeUseLocals(parameterContext, path.getParentPath(), type);
                }
                break;

            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();
                FlowExpressions.Receiver receiver =
                        FlowExpressions.internalReprOfPseudoReceiver(path, enclosingType);
                List<Receiver> params =
                        FlowExpressions.getParametersOfEnclosingMethod(factory, path);
                FlowExpressionContext localContext =
                        new FlowExpressionContext(receiver, params, factory.getContext());
                standardizeUseLocals(localContext, path, type);
                break;
            case FIELD:
            case ENUM_CONSTANT:
                FlowExpressions.Receiver receiverF;
                if (node.getKind() == Tree.Kind.IDENTIFIER) {
                    FlowExpressions.Receiver r =
                            FlowExpressions.internalReprOf(factory, (IdentifierTree) node);
                    receiverF =
                            r instanceof FlowExpressions.FieldAccess
                                    ? ((FlowExpressions.FieldAccess) r).getReceiver()
                                    : r;
                } else {
                    receiverF = FlowExpressions.internalReprOfImplicitReceiver(ele);
                }
                FlowExpressionContext fieldContext =
                        new FlowExpressionContext(receiverF, null, factory.getContext());
                standardizeDoNotUseLocals(fieldContext, path, type);
                break;
            default:
                throw new BugInCF(
                        this.getClass()
                                + ": unexpected element kind "
                                + ele.getKind()
                                + ": "
                                + ele);
        }
    }

    /** Standardize the Java expressions in annotations in a field access. */
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

        FlowExpressions.Receiver receiver =
                FlowExpressions.internalReprOf(factory, node.getExpression());
        FlowExpressionContext context =
                new FlowExpressionContext(receiver, null, factory.getContext());
        standardizeDoNotUseLocals(context, factory.getPath(node), type);
    }

    public void standardizeExpression(ExpressionTree tree, AnnotatedTypeMirror annotatedType) {
        if (!hasDependentType(annotatedType)) {
            return;
        }
        TreePath path = factory.getPath(tree);
        if (path == null) {
            return;
        }
        Tree enclosingClass = TreeUtils.enclosingClass(path);
        if (enclosingClass == null) {
            return;
        }
        TypeMirror enclosingType = TreeUtils.typeOf(enclosingClass);

        FlowExpressions.Receiver receiver =
                FlowExpressions.internalReprOfPseudoReceiver(path, enclosingType);

        FlowExpressionContext localContext =
                new FlowExpressionContext(
                        receiver,
                        FlowExpressions.getParametersOfEnclosingMethod(factory, path),
                        factory.getContext());
        standardizeUseLocals(localContext, path, annotatedType);
    }

    public void standardizeVariable(AnnotatedTypeMirror type, Element elt) {
        if (!hasDependentType(type)) {
            return;
        }

        switch (elt.getKind()) {
            case PARAMETER:
            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                Tree tree = factory.declarationFromElement(elt);
                if (tree == null) {
                    if (elt.getKind() == ElementKind.PARAMETER) {
                        // The tree might be null when
                        // org.checkerframework.framework.flow.CFAbstractTransfer.getValueFromFactory()
                        // gets the assignment context for a pseudo assignment of an argument to
                        // a method parameter.
                        return;
                    }
                    throw new BugInCF(this.getClass() + ": tree not found");
                } else if (TreeUtils.typeOf(tree) == null) {
                    // org.checkerframework.framework.flow.CFAbstractTransfer.getValueFromFactory()
                    // gets the assignment context for a pseudo assignment of an argument to
                    // a method parameter.
                    return;
                }

                standardizeVariable(tree, type, elt);
                break;
            default:
                // Nothing to do.
        }
    }

    private void standardizeUseLocals(
            FlowExpressionContext context, TreePath localScope, AnnotatedTypeMirror type) {
        standardizeAtm(context, localScope, type, true);
    }

    private void standardizeDoNotUseLocals(
            FlowExpressionContext context, TreePath localScope, AnnotatedTypeMirror type) {
        standardizeAtm(context, localScope, type, false);
    }

    private void standardizeAtm(
            FlowExpressionContext context,
            TreePath localScope,
            AnnotatedTypeMirror type,
            boolean useLocalScope) {
        // localScope is null in dataflow when creating synthetic trees for enhanced for loops.
        if (localScope != null) {
            new StandardizeTypeAnnotator(context, localScope, useLocalScope).visit(type);
        }
    }

    protected String standardizeString(
            String expression,
            FlowExpressionContext context,
            TreePath localScope,
            boolean useLocalScope) {
        if (DependentTypesError.isExpressionError(expression)) {
            return expression;
        }
        try {
            FlowExpressions.Receiver result =
                    FlowExpressionParseUtil.parse(expression, context, localScope, useLocalScope);
            if (result == null) {
                return new DependentTypesError(expression, " ").toString();
            }
            return result.toString();
        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            return new DependentTypesError(expression, e).toString();
        }
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
     * Standardizes Java expressions in an annotation. If the annotation is not a dependent type
     * annotation, returns the same annotation unchanged.
     *
     * @param context information about any receiver and arguments
     * @param localScope path to local scope to use
     * @param anno the annotation to be standardized
     * @param useLocalScope whether {@code localScope} should be used to resolve identifiers
     * @return the standardized annotation
     */
    public AnnotationMirror standardizeAnnotation(
            FlowExpressionContext context,
            TreePath localScope,
            AnnotationMirror anno,
            boolean useLocalScope) {
        if (!isExpressionAnno(anno)) {
            return anno;
        }
        return standardizeDependentTypeAnnotation(context, localScope, anno, useLocalScope);
    }

    /** Standardizes an annotation. If it is not a dependent type annotation, returns null. */
    private AnnotationMirror standardizeAnnotationIfDependentType(
            FlowExpressionContext context,
            TreePath localScope,
            AnnotationMirror anno,
            boolean useLocalScope) {
        if (!isExpressionAnno(anno)) {
            return null;
        }
        return standardizeDependentTypeAnnotation(context, localScope, anno, useLocalScope);
    }

    /** Standardizes a dependent type annotation. */
    private AnnotationMirror standardizeDependentTypeAnnotation(
            FlowExpressionContext context,
            TreePath localScope,
            AnnotationMirror anno,
            boolean useLocalScope) {
        AnnotationBuilder builder =
                new AnnotationBuilder(
                        factory.getProcessingEnv(), AnnotationUtils.annotationName(anno));

        for (String value : getListOfExpressionElements(anno)) {
            List<String> expressionStrings =
                    AnnotationUtils.getElementValueArray(anno, value, String.class, true);
            List<String> standardizedStrings = new ArrayList<>();
            for (String expression : expressionStrings) {
                standardizedStrings.add(
                        standardizeString(expression, context, localScope, useLocalScope));
            }
            builder.setValue(value, standardizedStrings);
        }
        return builder.build();
    }

    private class StandardizeTypeAnnotator extends AnnotatedTypeScanner<Void, Void> {
        private final FlowExpressionContext context;
        private final TreePath localScope;
        /** Whether or not the expression might contain a variable declared in local scope. */
        private final boolean useLocalScope;

        private StandardizeTypeAnnotator(
                FlowExpressionContext context, TreePath localScope, boolean useLocalScope) {
            this.context = context;
            this.localScope = localScope;
            this.useLocalScope = useLocalScope;
        }

        @Override
        public Void visitTypeVariable(AnnotatedTypeMirror.AnnotatedTypeVariable type, Void aVoid) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, null);

            // If the type variable has a primary annotation, then it is viewpoint adapted then
            // copied to the upper and lower bounds.  Attempting to viewpoint adapt again, could
            // cause the flow expression parser to fail.  So, remove the primary annotations from
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
            List<AnnotationMirror> newAnnos = new ArrayList<>();
            for (AnnotationMirror anno : type.getAnnotations()) {
                AnnotationMirror annotationMirror =
                        standardizeAnnotationIfDependentType(
                                context, localScope, anno, useLocalScope);
                if (annotationMirror != null) {
                    newAnnos.add(annotationMirror);
                }
            }
            for (AnnotationMirror anno : newAnnos) {
                // More than one annotation of the same class might have been written into
                // the element and therefore might appear more than once in the type.
                // See PR #674
                // https://github.com/typetools/checker-framework/pull/674
                // Work around this bug by remove all annotations of the same class.
                if (type.removeAnnotation(anno)) {
                    type.removeAnnotation(anno);
                }
            }
            type.addAnnotations(newAnnos);
            return super.scan(type, aVoid);
        }
    }

    /**
     * Checks all Java expressions in the given annotated type to see if the expression string is an
     * error string as specified by {@link DependentTypesError#isExpressionError}. If the annotated
     * type has any errors, a flowexpr.parse.error is issued at the errorTree.
     *
     * @param atm annotated type to check for expression errors
     * @param errorTree the tree at which to report any found errors
     */
    public void checkType(AnnotatedTypeMirror atm, Tree errorTree) {
        List<DependentTypesError> errors = new ExpressionErrorChecker().visit(atm);
        if (errors == null || errors.isEmpty()) {
            return;
        }
        if (errorTree.getKind() == Kind.VARIABLE) {
            ModifiersTree modifiers = ((VariableTree) errorTree).getModifiers();
            errorTree = ((VariableTree) errorTree).getType();
            for (AnnotationTree annoTree : modifiers.getAnnotations()) {
                for (Class<?> annoClazz : annoToElements.keySet()) {
                    if (annoTree.toString().contains(annoClazz.getSimpleName())) {
                        errorTree = annoTree;
                        break;
                    }
                }
            }
        }
        reportErrors(errorTree, errors);
    }

    protected void reportErrors(Tree errorTree, List<DependentTypesError> errors) {
        if (errors.isEmpty()) {
            return;
        }
        StringJoiner errorsFormatted = new StringJoiner(System.lineSeparator());
        for (DependentTypesError dte : errors) {
            errorsFormatted.add(dte.format());
        }
        SourceChecker checker = factory.getContext().getChecker();
        checker.reportError(
                errorTree, "expression.unparsable.type.invalid", errorsFormatted.toString());
    }

    /**
     * Checks every Java expression element of the annotation to see if the expression is an error
     * string as specified by DependentTypesError#isExpressionError. If any expression is an error,
     * then a non-empty list of {@link DependentTypesError} is returned.
     */
    private List<DependentTypesError> checkForError(AnnotationMirror am) {
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
     * then an error is reported at {@code errorTree}.
     *
     * @param annotation annotation to check
     * @param errorTree location at which to issue errors
     */
    public void checkAnnotation(AnnotationMirror annotation, Tree errorTree) {
        List<DependentTypesError> errors = checkForError(annotation);
        if (errors.isEmpty()) {
            return;
        }
        SourceChecker checker = factory.getContext().getChecker();
        String error = UtilPlume.joinLines(errors);
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
        // TODO: check that invalid annotations in type variable bounds are properly
        // formatted. They are part of the type, but the output isn't nicely formatted.
        checkType(type, classTree);
    }

    /**
     * Checks all Java expressions in the method declaration AnnotatedTypeMirror to see if the
     * expression string is an error string as specified by DependentTypesError#isExpressionError.
     * If the annotated type has any errors, a flowexpr.parse.error is issued.
     *
     * @param methodTree method to check
     * @param type annotated type of the method
     */
    public void checkMethod(MethodTree methodTree, AnnotatedExecutableType type) {
        // Parameters and receivers are checked by visitVariable
        // So only type parameters and return type need to be checked here.
        checkTypeVariables(methodTree, type);

        // Check return type
        if (type.getReturnType().getKind() != TypeKind.VOID) {
            AnnotatedTypeMirror returnType = factory.getMethodReturnType(methodTree);
            Tree treeForError =
                    TreeUtils.isConstructor(methodTree) ? methodTree : methodTree.getReturnType();
            checkType(returnType, treeForError);
        }
    }

    private void checkTypeVariables(MethodTree node, AnnotatedExecutableType methodType) {
        Element ele = TreeUtils.elementFromDeclaration(node);
        TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();

        FlowExpressionContext context =
                FlowExpressionContext.buildContextForMethodDeclaration(
                        node, enclosingType, factory.getContext());
        for (int i = 0; i < methodType.getTypeVariables().size(); i++) {
            AnnotatedTypeMirror atm = methodType.getTypeVariables().get(i);
            standardizeDoNotUseLocals(context, factory.getPath(node), atm);
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
                                errors.addAll(checkForError(am));
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

    /** Copies annotations that might have been viewpoint adapted from type to the parameter. */
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
     * Whether or not atm has any dependent type annotations. If an annotated type does not have a
     * dependent type annotation, then no standardization or viewpoint adaption is performed. (This
     * check avoids calling time intensive methods unless absolutely required.)
     */
    private boolean hasDependentType(AnnotatedTypeMirror atm) {
        if (atm == null) {
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
     * Returns the list of elements of the annotation that are Java expressions, or the empty list
     * if there aren't any.
     *
     * @param am AnnotationMirror
     * @return the list of elements of the annotation that are Java expressions, or the empty list
     *     if there aren't any
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
