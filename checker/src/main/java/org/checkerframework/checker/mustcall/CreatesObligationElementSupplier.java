package org.checkerframework.checker.mustcall;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.mustcall.qual.CreatesObligation;
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
 * #getCreatesObligationExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory,
 * CreatesObligationElementSupplier)} is called. This interface is needed so any type factory with
 * these elements can be used to call that method, not just the MustCallAnnotatedTypeFactory (in
 * particular, the consistency checker needs to be able to call that method with both the
 * CalledMethods type factory and the MustCall type factory).
 */
public interface CreatesObligationElementSupplier {

  /**
   * Returns the CreatesObligation.value field/element.
   *
   * @return the CreatesObligation.value field/element
   */
  ExecutableElement getCreatesObligationValueElement();

  /**
   * Returns the CreatesObligation.List.value field/element.
   *
   * @return the CreatesObligation.List.value field/element
   */
  ExecutableElement getCreatesObligationListValueElement();

  /**
   * Returns the arguments of the @CreatesObligation annotation on the invoked method, as
   * JavaExpressions. Returns the empty set if the given method has no @CreatesObligation
   * annotation.
   *
   * <p>If any expression is unparseable, this method reports an error and returns the empty set.
   *
   * @param n a method invocation
   * @param atypeFactory the type factory to report errors and parse the expression string
   * @param supplier a type factory that can supply the executable elements for CreatesObligation
   *     and CreatesObligation.List's value elements. Usually, you should just pass atypeFactory
   *     again. The arguments are different so that the given type factory's adherence to both
   *     protocols are checked by the type system.
   * @return the arguments of the method's @CreatesObligation annotation, or an empty list
   */
  static List<JavaExpression> getCreatesObligationExpressions(
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      CreatesObligationElementSupplier supplier) {
    AnnotationMirror createsObligationList =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesObligation.List.class);
    List<JavaExpression> results = new ArrayList<>(1);
    if (createsObligationList != null) {
      // Handle a set of CreatesObligation annotations.
      List<AnnotationMirror> createsObligations =
          AnnotationUtils.getElementValueArray(
              createsObligationList,
              supplier.getCreatesObligationListValueElement(),
              AnnotationMirror.class);
      for (AnnotationMirror co : createsObligations) {
        JavaExpression expr = getCreatesObligationExpression(co, n, atypeFactory, supplier);
        if (expr != null && !results.contains(expr)) {
          results.add(expr);
        }
      }
    }
    AnnotationMirror createsObligation =
        atypeFactory.getDeclAnnotation(n.getTarget().getMethod(), CreatesObligation.class);
    if (createsObligation != null) {
      JavaExpression expr =
          getCreatesObligationExpression(createsObligation, n, atypeFactory, supplier);
      if (expr != null && !results.contains(expr)) {
        results.add(expr);
      }
    }
    return results;
  }

  /**
   * Parses a single CreatesObligation annotation. Clients should use {@link
   * #getCreatesObligationExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory,
   * CreatesObligationElementSupplier)}, which handles the possibility of multiple such annotations,
   * instead.
   *
   * @param createsObligation a @CreatesObligation annotation
   * @param n the invocation of a reset method
   * @param atypeFactory the type factory
   * @param supplier a type factory that can supply the executable elements for CreatesObligation
   *     and CreatesObligation.List's value elements. Usually, you should just pass atypeFactory
   *     again. The arguments are different so that the given type factory's adherence to both
   *     protocols are checked by the type system.
   * @return the Java expression representing the target, or null if the target is unparseable
   */
  static @Nullable JavaExpression getCreatesObligationExpression(
      AnnotationMirror createsObligation,
      MethodInvocationNode n,
      GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory,
      CreatesObligationElementSupplier supplier) {
    // Unfortunately, there is no way to avoid passing the default string "this" here. The default
    // must be hard-coded into the client, such as here. That is the price for the efficiency of not
    // having to query the annotation definition (such queries are expensive).
    String targetStrWithoutAdaptation =
        AnnotationUtils.getElementValue(
            createsObligation, supplier.getCreatesObligationValueElement(), String.class, "this");
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
   * Issues a createsobligation.target.unparseable error.
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
            "createsobligation.target.unparseable",
            n.getTarget().getMethod().getSimpleName(),
            unparseable);
  }
}
