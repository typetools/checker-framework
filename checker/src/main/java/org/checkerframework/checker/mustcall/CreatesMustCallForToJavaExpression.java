package org.checkerframework.checker.mustcall;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;

/**
 * Utility methods to convert targets of {@code @CreatesMustCallFor} annotations to {@link
 * org.checkerframework.dataflow.expression.JavaExpression}s.
 */
public class CreatesMustCallForToJavaExpression {

    /** static utility methods only; don't create instances */
    private CreatesMustCallForToJavaExpression() {}

    /**
     * Returns the elements of the @CreatesMustCallFor annotations on the invoked method, as
     * JavaExpressions. Returns the empty set if the given method has no @CreatesMustCallFor
     * annotation.
     *
     * <p>If any expression is unparseable, this method reports an error and returns the empty set.
     *
     * @param n a method invocation
     * @param atypeFactory the type factory to report errors and parse the expression string
     * @param supplier a type factory that can supply the executable elements for CreatesMustCallFor
     *     and CreatesMustCallFor.List's value elements. Usually, you should just pass atypeFactory
     *     again. The arguments are different so that the given type factory's adherence to both
     *     protocols are checked by the type system.
     * @return the arguments of the method's @CreatesMustCallFor annotation, or an empty list
     */
    public static List<JavaExpression> getCreatesMustCallForExpressionsAtInvocation(
            MethodInvocationNode n,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
            CreatesMustCallForElementSupplier supplier) {
        List<JavaExpression> results = new ArrayList<>(1);
        List<AnnotationMirror> createsMustCallForAnnos =
                getCreatesMustCallForAnnos(n.getTarget().getMethod(), atypeFactory, supplier);
        for (AnnotationMirror createsMustCallFor : createsMustCallForAnnos) {
            JavaExpression expr =
                    getCreatesMustCallForExpression(
                            createsMustCallFor,
                            n.getTree(),
                            n.getTarget().getMethod().getSimpleName(),
                            atypeFactory,
                            supplier,
                            (s) ->
                                    StringToJavaExpression.atMethodInvocation(
                                            s, n, atypeFactory.getChecker()));
            if (expr != null && !results.contains(expr)) {
                results.add(expr);
            }
        }
        return results;
    }

    /**
     * Returns the elements of the @CreatesMustCallFor annotations on the method declaration, as
     * JavaExpressions. Returns the empty set if the given method has no @CreatesMustCallFor
     * annotation.
     *
     * <p>If any expression is unparseable, this method reports an error and returns the empty set.
     *
     * @param tree a method declaration
     * @param atypeFactory the type factory to report errors and parse the expression string
     * @param supplier a type factory that can supply the executable elements for CreatesMustCallFor
     *     and CreatesMustCallFor.List's value elements. Usually, you should just pass atypeFactory
     *     again. The arguments are different so that the given type factory's adherence to both
     *     protocols are checked by the type system.
     * @return the arguments of the method's @CreatesMustCallFor annotation, or an empty list
     */
    public static List<JavaExpression> getCreatesMustCallForExpressionsAtMethodDeclaration(
            MethodTree tree,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
            CreatesMustCallForElementSupplier supplier) {
        List<JavaExpression> results = new ArrayList<>(1);
        ExecutableElement method = TreeUtils.elementFromDeclaration(tree);
        List<AnnotationMirror> createsMustCallForAnnos =
                getCreatesMustCallForAnnos(method, atypeFactory, supplier);
        for (AnnotationMirror createsMustCallFor : createsMustCallForAnnos) {
            JavaExpression expr =
                    getCreatesMustCallForExpression(
                            createsMustCallFor,
                            tree,
                            method.getSimpleName(),
                            atypeFactory,
                            supplier,
                            (s) ->
                                    StringToJavaExpression.atMethodBody(
                                            s, tree, atypeFactory.getChecker()));
            if (expr != null && !results.contains(expr)) {
                results.add(expr);
            }
        }
        return results;
    }

    /**
     * Returns the {@code CreatesMustCallFor} annotations on a method
     *
     * @param method the method
     * @param atypeFactory the type factory to use for looking up annotations
     * @param supplier supplier to use to get elements
     * @return the {@code CreatesMustCallFor} annotations
     */
    private static List<AnnotationMirror> getCreatesMustCallForAnnos(
            ExecutableElement method,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
            CreatesMustCallForElementSupplier supplier) {
        AnnotationMirror createsMustCallForList =
                atypeFactory.getDeclAnnotation(method, CreatesMustCallFor.List.class);
        List<AnnotationMirror> result = new ArrayList<>();
        if (createsMustCallForList != null) {
            // Handle a set of CreatesMustCallFor annotations.
            result.addAll(
                    AnnotationUtils.getElementValueArray(
                            createsMustCallForList,
                            supplier.getCreatesMustCallForListValueElement(),
                            AnnotationMirror.class));
        }
        AnnotationMirror createsMustCallFor =
                atypeFactory.getDeclAnnotation(method, CreatesMustCallFor.class);
        if (createsMustCallFor != null) {
            result.add(createsMustCallFor);
        }
        return result;
    }

    /**
     * Parses a single CreatesMustCallFor annotation. If the target expression cannot be parsed,
     * issues a checker error.
     *
     * @param createsMustCallFor a @CreatesMustCallFor annotation
     * @param tree the tree on which to report an error if annotation cannot be parsed
     * @param methodName name to use in error message if annotation cannot be parsed
     * @param atypeFactory the type factory
     * @param supplier a type factory that can supply the executable elements for CreatesMustCallFor
     *     and CreatesMustCallFor.List's value elements. Usually, you should just pass atypeFactory
     *     again. The arguments are different so that the given type factory's adherence to both
     *     protocols are checked by the type system.
     * @param converter function to be used to create JavaExpression
     * @return the Java expression representing the target, or null if the target is unparseable
     */
    private static @Nullable JavaExpression getCreatesMustCallForExpression(
            AnnotationMirror createsMustCallFor,
            Tree tree,
            Name methodName,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
            CreatesMustCallForElementSupplier supplier,
            StringToJavaExpression converter) {
        // Unfortunately, there is no way to avoid passing the default string "this" here. The
        // default must be hard-coded into the client, such as here. That is the price for the
        // efficiency of not having to query the annotation definition (such queries are expensive).
        String targetStrWithoutAdaptation =
                AnnotationUtils.getElementValue(
                        createsMustCallFor,
                        supplier.getCreatesMustCallForValueElement(),
                        String.class,
                        "this");
        // TODO: find a way to also check if the target is a known tempvar, and if so return that.
        // That should improve the quality of the error messages we give.
        JavaExpression targetExpr;
        try {
            targetExpr = converter.toJavaExpression(targetStrWithoutAdaptation);
            if (targetExpr instanceof Unknown) {
                issueUnparseableError(tree, methodName, atypeFactory, targetStrWithoutAdaptation);
                return null;
            }
        } catch (JavaExpressionParseUtil.JavaExpressionParseException e) {
            issueUnparseableError(tree, methodName, atypeFactory, targetStrWithoutAdaptation);
            return null;
        }
        return targetExpr;
    }

    /**
     * Issues a createsmustcallfor.target.unparseable error.
     *
     * @param tree the tree on which to report the error
     * @param methodName method name to use in error message
     * @param atypeFactory the type factory to use to issue the error
     * @param unparseable the unparseable string
     */
    private static void issueUnparseableError(
            Tree tree,
            Name methodName,
            GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
            String unparseable) {
        atypeFactory
                .getChecker()
                .reportError(
                        tree, "createsmustcallfor.target.unparseable", methodName, unparseable);
    }
}
