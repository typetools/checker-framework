package org.checkerframework.framework.testchecker.testaccumulation;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Wrapper for TestAccumulationAnnotatedTypeFactory for a version of the checker without returns
 * receiver support, to enable the checker's auto-discovery of its AnnotatedTypeFactory to succeed.
 */
public class TestAccumulationNoReturnsReceiverAnnotatedTypeFactory
    extends TestAccumulationAnnotatedTypeFactory {
  /**
   * Creates a new accumulation checker's annotated type factory.
   *
   * @param checker the checker
   */
  public TestAccumulationNoReturnsReceiverAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
  }
}
