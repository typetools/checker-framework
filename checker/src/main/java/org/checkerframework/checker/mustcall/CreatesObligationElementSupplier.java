package org.checkerframework.checker.mustcall;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * This interface should be implemented by all type factories that can provide the
 * ExecutableElement needed to call {@link AnnotationUtils#getElementValueArray} when
 * {@link MustCallTransfer#getCreatesObligationExpressions(MethodInvocationNode, GenericAnnotatedTypeFactory)
 * is called.
 */
public interface CreatesObligationElementSupplier {

  /**
   * Get the element for a single CreatesObligation annotation.
   *
   * @return the element
   */
  ExecutableElement getCreatesObligationValueElement();

  /**
   * Get the element for a list CreatesObligation annotation.
   *
   * @return the element
   */
  ExecutableElement getCreatesObligationListValueElement();
}
