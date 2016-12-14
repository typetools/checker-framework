package org.checkerframework.framework.util.expressionannotations;

import com.sun.source.tree.AnnotationTree;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.visitor.AnnotatedTypeComparer;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A class that helps checkers use qualifiers that are represented by annotations with Java
 * expression strings. This class performs four main functions:
 *
 * <p>1. Standardizes/canonicalizes the expressions in the annotations such that two expression
 * strings that are equivalent are made to be equal. For example, an instance field f may appear in
 * an expression string as "f" or "this.f"; this class standardizes both strings to "this.f".
 *
 * <p>2. Viewpoint-adapts annotations on field or method declarations at field accesses or method
 * invocations.
 *
 * <p>3. Changes invalid expression strings to an error string that includes the reason why the
 * expression is invalid. For example, {@code @KeyFor("m")} would be changed to
 * {@code @KeyFor("[error for expression: m error: m: identifier not found]")} if m is not a valid
 * identifier.
 *
 * <p>4. Checks annotated types for error strings that have been added by this class and issues an
 * error if any are found.
 *
 * <p>Steps 3 and 4 are separated so that an error is issued only once per invalid expression string
 * rather than every time the expression string is parsed. (The expression string is parsed multiple
 * times because annotated types are created multiple times.)
 */
public class ExpressionAnnotationHelper {
    protected final AnnotatedTypeFactory factory;
    /** A list of annotations that are expression annotations. */
    protected final List<Class<? extends Annotation>> expressionAnnos;

    public ExpressionAnnotationHelper(
            AnnotatedTypeFactory factory, List<Class<? extends Annotation>> expressionAnnos) {
        this(factory, null, expressionAnnos);
    }

    public ExpressionAnnotationHelper(
            AnnotatedTypeFactory factory, Class<? extends Annotation> anno) {
        this(factory, anno, null);
    }

    private ExpressionAnnotationHelper(
            AnnotatedTypeFactory factory,
            Class<? extends Annotation> anno,
            List<Class<? extends Annotation>> expressionAnnos) {
        this.factory = factory;
        if (expressionAnnos == null) {
            expressionAnnos = new ArrayList<>();
            expressionAnnos.add(anno);
        }
        this.expressionAnnos = expressionAnnos;
    }

    /**
     * Creates a TreeAnnotator that standarizes expression annotations.
     *
     * @param factory annotated type factory
     * @return a new TreeAnnotator that standarizes expression annotatoions
     */
    public TreeAnnotator createExpressionAnnotationTreeAnnotator(AnnotatedTypeFactory factory) {
        return new ExpressionAnnotationTreeAnnotator(factory, this);
    }

    /**
     * Viewpoint adapts the expression annotations on the bounds to the use of the type.
     *
     * @param classDecl class or interface declaration whose type variables should be viewpoint
     *     adapted
     * @param bounds annotated types of the bounds of the type variables.
     * @param pathToUse tree path to the use of the class or interface
     */
    public void viewpointAdaptTypeVariableBounds(
            TypeElement classDecl, List<AnnotatedTypeParameterBounds> bounds, TreePath pathToUse) {
        FlowExpressions.Receiver r = FlowExpressions.internalRepOfImplicitReceiver(classDecl);
        FlowExpressionContext context = new FlowExpressionContext(r, null, factory.getContext());
        for (AnnotatedTypeParameterBounds bound : bounds) {
            standardizeDoNotUseLocals(context, pathToUse, bound.getUpperBound());
            standardizeDoNotUseLocals(context, pathToUse, bound.getLowerBound());
        }
    }

    /**
     * Viewpoint adapts the expression annotations in the methodDeclType based on the
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
     * Viewpoint adapts the expression annotations in the constructorType based on the newClassTree.
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
        if (!hasExpressionAnnotation(viewpointAdaptedType)) {
            return;
        }

        FlowExpressions.Receiver receiver;
        if (receiverTree == null) {
            receiver =
                    FlowExpressions.internalRepOfImplicitReceiver(TreeUtils.elementFromUse(tree));
        } else {
            receiver = FlowExpressions.internalReprOf(factory, receiverTree);
        }

        List<FlowExpressions.Receiver> argReceivers = new ArrayList<>(args.size());
        for (ExpressionTree argTree : args) {
            argReceivers.add(FlowExpressions.internalReprOf(factory, argTree));
        }

        TreePath currentPath = factory.getPath(tree);

        FlowExpressionContext context =
                new FlowExpressionContext(receiver, argReceivers, factory.getContext());

        // typeForUse cannot be viewpoint adapted directly because it is the type post type variable
        // substitution.  Expression annotations on type arguments do not (and cannot) be viewpoint
        // adapted along with the expression annotations that are on the method declaration.
        // For example:
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

    public void standardizeNewClassTree(NewClassTree tree, AnnotatedDeclaredType type) {
        if (!hasExpressionAnnotation(type)) {
            return;
        }

        TreePath path = factory.getPath(tree);
        FlowExpressions.Receiver r =
                FlowExpressions.internalRepOfImplicitReceiver(TreeUtils.elementFromUse(tree));
        FlowExpressionContext context =
                new FlowExpressionContext(
                        r,
                        FlowExpressions.getParametersOfEnclosingMethod(factory, path),
                        factory.getContext());
        standardizeUseLocals(context, path, type);
    }

    public void standardizeReturnType(MethodTree m, AnnotatedTypeMirror atm) {
        if (atm.getKind() == TypeKind.NONE) {
            return;
        }
        if (!hasExpressionAnnotation(atm)) {
            return;
        }

        Element ele = TreeUtils.elementFromDeclaration(m);
        TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();

        FlowExpressionContext context =
                FlowExpressionContext.buildContextForMethodDeclaration(
                        m, enclosingType, factory.getContext());
        standardizeDoNotUseLocals(context, factory.getPath(m), atm);
    }

    public void standardizeVariable(Tree node, AnnotatedTypeMirror type, Element ele) {
        if (!hasExpressionAnnotation(type)) {
            return;
        }

        TreePath path = factory.getPath(node);
        if (path == null) {
            return;
        }
        switch (ele.getKind()) {
            case PARAMETER:
                MethodTree methodTree = TreeUtils.enclosingMethod(path);
                if (methodTree != null) {
                    FlowExpressionContext parameterContext =
                            FlowExpressionContext.buildContextForMethodDeclaration(
                                    methodTree, path, factory.getContext());
                    standardizeDoNotUseLocals(parameterContext, path, type);
                    break;
                }
                // If there is no enclosing method, then the parameter is a parameter to a lambda
                LambdaExpressionTree lambdaTree =
                        (LambdaExpressionTree)
                                TreeUtils.enclosingOfKind(path, Tree.Kind.LAMBDA_EXPRESSION);
                FlowExpressionContext parameterContext =
                        FlowExpressionContext.buildContextForLambda(
                                lambdaTree, path, factory.getContext());
                // TODO: test this.
                standardizeUseLocals(parameterContext, path, type);
                break;

            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();
                FlowExpressions.Receiver receiver =
                        FlowExpressions.internalRepOfPseudoReceiver(path, enclosingType);
                List<Receiver> params =
                        FlowExpressions.getParametersOfEnclosingMethod(factory, path);
                FlowExpressionContext localContext =
                        new FlowExpressionContext(receiver, params, factory.getContext());
                standardizeUseLocals(localContext, path, type);
                break;
            case FIELD:
                FlowExpressions.Receiver receiverF;
                if (node.getKind() == Tree.Kind.IDENTIFIER) {
                    FlowExpressions.Receiver r =
                            FlowExpressions.internalReprOf(factory, (IdentifierTree) node);
                    receiverF =
                            r instanceof FlowExpressions.FieldAccess
                                    ? ((FlowExpressions.FieldAccess) r).getReceiver()
                                    : r;
                } else {
                    receiverF = FlowExpressions.internalRepOfImplicitReceiver(ele);
                }
                FlowExpressionContext fieldContext =
                        new FlowExpressionContext(receiverF, null, factory.getContext());
                standardizeDoNotUseLocals(fieldContext, path, type);
                break;
        }
    }

    public void standardizeFieldAccess(MemberSelectTree node, AnnotatedTypeMirror type) {
        if (!hasExpressionAnnotation(type)) {
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
        if (!hasExpressionAnnotation(annotatedType)) {
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
        TypeMirror enclosingType = InternalUtils.typeOf(enclosingClass);

        FlowExpressions.Receiver receiver =
                FlowExpressions.internalRepOfPseudoReceiver(path, enclosingType);

        FlowExpressionContext localContext =
                new FlowExpressionContext(
                        receiver,
                        FlowExpressions.getParametersOfEnclosingMethod(factory, path),
                        factory.getContext());
        standardizeUseLocals(localContext, path, annotatedType);
    }

    public void standardizeVariable(AnnotatedTypeMirror type, Element elt) {
        if (!hasExpressionAnnotation(type)) {
            return;
        }

        switch (elt.getKind()) {
            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                Tree tree = factory.declarationFromElement(elt);
                if (tree == null) {
                    ErrorReporter.errorAbort(this.getClass() + ": tree not found");
                }
                standardizeVariable(tree, type, elt);
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
        if (ExpressionAnnotationError.isExpressionError(expression)) {
            return expression;
        }
        try {
            FlowExpressions.Receiver result =
                    FlowExpressionParseUtil.parse(expression, context, localScope, useLocalScope);
            if (result == null) {
                return new ExpressionAnnotationError(expression, " ").toString();
            }
            return result.toString();
        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            return new ExpressionAnnotationError(expression, e).toString();
        }
    }

    private class StandardizeTypeAnnotator extends AnnotatedTypeScanner<Void, Void> {
        private final FlowExpressionContext context;
        private final TreePath localScope;
        /** Whether or not the expression might contain a variable declared in local scope */
        private final boolean useLocalScope;

        private StandardizeTypeAnnotator(
                FlowExpressionContext context, TreePath localScope, boolean useLocalScope) {
            this.context = context;
            this.localScope = localScope;
            this.useLocalScope = useLocalScope;
        }

        private AnnotationMirror standardizeAnnotation(
                FlowExpressionContext context,
                TreePath localScope,
                AnnotationMirror anno,
                boolean useLocalScope) {
            if (!isExpressionAnno(anno)) {
                return null;
            }
            List<String> expressionStrings =
                    AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
            List<String> vpdStrings = new ArrayList<>();
            for (String expression : expressionStrings) {
                vpdStrings.add(standardizeString(expression, context, localScope, useLocalScope));
            }
            AnnotationBuilder builder =
                    new AnnotationBuilder(
                            factory.getProcessingEnv(), AnnotationUtils.annotationName(anno));
            builder.setValue("value", vpdStrings);
            return builder.build();
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
            if (type == null) {
                return null;
            }
            List<AnnotationMirror> newAnnos = new ArrayList<>();
            for (AnnotationMirror anno : type.getAnnotations()) {
                AnnotationMirror annotationMirror =
                        standardizeAnnotation(context, localScope, anno, useLocalScope);
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
     * Checks all expression annotations in the given annotated type to see if the expression string
     * is an error string as specified by ExpressionAnnotationError#isExpressionError. If the
     * annotated type has any errors, a flowexpr.parse.error is issued at the errorTree.
     *
     * @param atm annotated type to check for expression errors
     * @param errorTree the tree at which to report any found errors
     */
    public void checkType(AnnotatedTypeMirror atm, Tree errorTree) {
        List<ExpressionAnnotationError> errors = new ExpressionErrorChecker().visit(atm);
        if (errors == null || errors.isEmpty()) {
            return;
        }
        if (errorTree.getKind() == Kind.VARIABLE) {
            ModifiersTree modifiers = ((VariableTree) errorTree).getModifiers();
            errorTree = ((VariableTree) errorTree).getType();
            for (AnnotationTree annoTree : modifiers.getAnnotations()) {
                for (Class<?> annoClazz : expressionAnnos) {
                    if (annoTree.toString().contains(annoClazz.getSimpleName())) {
                        errorTree = annoTree;
                        break;
                    }
                }
            }
        }
        reportErrors(errorTree, errors);
    }

    protected void reportErrors(Tree errorTree, List<ExpressionAnnotationError> errors) {
        if (errors.isEmpty()) {
            return;
        }
        SourceChecker checker = factory.getContext().getChecker();
        String error = PluginUtil.join("\n", errors);
        checker.report(Result.failure("expression.unparsable.type.invalid", error), errorTree);
    }

    /**
     * Checks all expression annotations in the method declaration to see if the expression string
     * is an error string as specified by ExpressionAnnotationError#isExpressionError. If the
     * annotated type has any errors, a flowexpr.parse.error is issued.
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
            checkType(returnType, methodTree.getReturnType());
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

    boolean isExpressionAnno(AnnotationMirror am) {
        for (Class<? extends Annotation> clazz : expressionAnnos) {
            if (AnnotationUtils.areSameByClass(am, clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks all expression annotations in the given annotated type to see if the expression string
     * is an error string as specified by ExpressionAnnotationError#isExpressionError. If the
     * annotated type has any errors, then a non-empty list of {@link ExpressionAnnotationError} is
     * returned.
     */
    private class ExpressionErrorChecker
            extends AnnotatedTypeScanner<List<ExpressionAnnotationError>, Void> {

        @Override
        protected List<ExpressionAnnotationError> scan(AnnotatedTypeMirror type, Void aVoid) {
            if (type == null) {
                return super.scan(type, aVoid);
            }
            List<ExpressionAnnotationError> errors = new ArrayList<>();
            for (AnnotationMirror am : type.getAnnotations()) {
                if (isExpressionAnno(am)) {
                    errors.addAll(checkForError(am));
                }
            }
            List<ExpressionAnnotationError> superList = super.scan(type, aVoid);
            if (superList != null) {
                errors.addAll(superList);
            }
            return errors;
        }

        @Override
        protected List<ExpressionAnnotationError> reduce(
                List<ExpressionAnnotationError> r1, List<ExpressionAnnotationError> r2) {
            if (r1 != null && r2 != null) {
                r1.addAll(r2);
                return r1;
            } else if (r1 != null) {
                return r1;
            } else if (r2 != null) {
                return r2;
            } else {
                return null;
            }
        }

        private List<ExpressionAnnotationError> checkForError(AnnotationMirror am) {
            List<String> value =
                    AnnotationUtils.getElementValueArray(am, "value", String.class, true);
            List<ExpressionAnnotationError> errors = new ArrayList<>();
            for (String v : value) {
                if (ExpressionAnnotationError.isExpressionError(v)) {
                    errors.add(new ExpressionAnnotationError(v));
                }
            }
            return errors;
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
            for (Class<? extends Annotation> vpa : expressionAnnos) {
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
                ErrorReporter.errorAbort("Should be the same. type: %s p: %s ", type, p);
            }
            return null;
        }

        @Override
        protected Void combineRs(Void r1, Void r2) {
            return null;
        }
    }

    /**
     * Whether or not atm has an expression annotation. If an annotated type does not have an
     * expression annotation, then no standardization or viewpoint adaption is performed. (This
     * check avoids calling time intensive methods unless absolutely required.)
     */
    private boolean hasExpressionAnnotation(AnnotatedTypeMirror atm) {
        if (atm == null) {
            return false;
        }
        Boolean b = new ExpressionAnnotationExists().visit(atm);
        if (b == null) {
            return false;
        }
        return b;
    }

    /** Checks whether or not an annotated type contains an expression annotation. */
    private class ExpressionAnnotationExists extends AnnotatedTypeScanner<Boolean, Void> {
        @Override
        protected Boolean scan(AnnotatedTypeMirror type, Void aVoid) {
            if (type == null) {
                return false;
            }
            for (AnnotationMirror am : type.getAnnotations()) {
                if (isExpressionAnno(am)) {
                    return true;
                }
            }
            return super.scan(type, aVoid);
        }

        @Override
        protected Boolean reduce(Boolean r1, Boolean r2) {
            if (r1 != null && r2 != null) {
                // if either have an expression anno, return true;
                return r1 || r2;
            } else if (r1 != null) {
                return r1;
            } else if (r2 != null) {
                return r2;
            } else {
                return false;
            }
        }
    }
}
