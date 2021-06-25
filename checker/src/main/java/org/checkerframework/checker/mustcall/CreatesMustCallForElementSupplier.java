package org.checkerframework.checker.mustcall;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This interface should be implemented by all type factories that can provide the ExecutableElement
 * needed to call {@link AnnotationUtils#getElementValueArray} when {@link
 * #getCreatesMustCallForExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory,
 * CreatesMustCallForElementSupplier)} is called. This interface is needed so any type factory with
 * these elements can be used to call that method, not just the MustCallAnnotatedTypeFactory (in
 * particular, the consistency checker needs to be able to call that method with both the
 * CalledMethods type factory and the MustCall type factory).
 */
public interface CreatesMustCallForElementSupplier {

  /**
   * Returns the CreatesMustCallFor.value field/element.
   *
   * @return the CreatesMustCallFor.value field/element
   */
  ExecutableElement getCreatesMustCallForValueElement();

  /**
   * Returns the CreatesMustCallFor.List.value field/element.
   *
   * @return the CreatesMustCallFor.List.value field/element
   */
  ExecutableElement getCreatesMustCallForListValueElement();

  /**
   * Returns the arguments of the @CreatesMustCallFor annotation on the invoked method, as
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
  static List<JavaExpression> getCreatesMustCallForExpressions(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      CreatesMustCallForElementSupplier supplier) {
    AnnotationMirror createsMustCallForList =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesMustCallFor.List.class);
    List<JavaExpression> results = new ArrayList<>(1);
    if (createsMustCallForList != null) {
      // Handle a set of CreatesMustCallFor annotations.
      List<AnnotationMirror> createsMustCallForAnnos =
          AnnotationUtils.getElementValueArray(
              createsMustCallForList,
              supplier.getCreatesMustCallForListValueElement(),
              AnnotationMirror.class);
      for (AnnotationMirror createsMustCallFor : createsMustCallForAnnos) {
        JavaExpression expr =
            getCreatesMustCallForExpression(createsMustCallFor, n, atypeFactory, supplier);
        if (expr != null && !results.contains(expr)) {
          results.add(expr);
        }
      }
    }
    AnnotationMirror createsMustCallFor =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesMustCallFor.class);
    if (createsMustCallFor != null) {
      JavaExpression expr =
          getCreatesMustCallForExpression(createsMustCallFor, n, atypeFactory, supplier);
      if (expr != null && !results.contains(expr)) {
        results.add(expr);
      }
    }
    return results;
  }

  /**
   * Parses a single CreatesMustCallFor annotation. Clients should use {@link
   * #getCreatesMustCallForExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory,
   * CreatesMustCallForElementSupplier)}, which handles the possibility of multiple such
   * annotations, instead.
   *
   * @param createsMustCallFor a @CreatesMustCallFor annotation
   * @param n the invocation of a reset method
   * @param atypeFactory the type factory
   * @param supplier a type factory that can supply the executable elements for CreatesMustCallFor
   *     and CreatesMustCallFor.List's value elements. Usually, you should just pass atypeFactory
   *     again. The arguments are different so that the given type factory's adherence to both
   *     protocols are checked by the type system.
   * @return the Java expression representing the target, or null if the target is unparseable
   */
  static @Nullable JavaExpression getCreatesMustCallForExpression(
      AnnotationMirror createsMustCallFor,
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      CreatesMustCallForElementSupplier supplier) {
    // Unfortunately, there is no way to avoid passing the default string "this" here. The default
    // must be hard-coded into the client, such as here. That is the price for the efficiency of not
    // having to query the annotation definition (such queries are expensive).
    String targetStrWithoutAdaptation =
        AnnotationUtils.getElementValue(
            createsMustCallFor, supplier.getCreatesMustCallForValueElement(), String.class, "this");
    // TODO: find a way to also check if the target is a known tempvar, and if so return that. That
    // should improve the quality of the error messages we give.
    JavaExpression targetExpr;
    try {
      targetExpr =
          StringToJavaExpression.atMethodInvocation(
              targetStrWithoutAdaptation, n, atypeFactory.getChecker());
      if (targetExpr instanceof Unknown) {
        issueUnparseableError(n, atypeFactory, targetStrWithoutAdaptation);
        return null;
      }
    } catch (JavaExpressionParseException e) {
      issueUnparseableError(n, atypeFactory, targetStrWithoutAdaptation);
      return null;
    }
    return targetExpr;
  }

  /**
   * Issues a createsmustcallfor.target.unparseable error.
   *
   * @param n the node
   * @param atypeFactory the type factory to use to issue the error
   * @param unparseable the unparseable string
   */
  static void issueUnparseableError(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      String unparseable) {
    atypeFactory
        .getChecker()
        .reportError(
            n.getTree(),
            "createsmustcallfor.target.unparseable",
            n.getTarget().getMethod().getSimpleName(),
            unparseable);
  }
}
