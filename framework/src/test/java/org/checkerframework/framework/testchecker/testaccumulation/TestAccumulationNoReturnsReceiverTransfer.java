package org.checkerframework.framework.testchecker.testaccumulation;

import org.checkerframework.framework.flow.CFAnalysis;

/**
 * Wrapper for TestAccumulationTransfer so that checker auto-discovery works for the version of the
 * checker without support for the Returns Receiver Checker.
 */
public class TestAccumulationNoReturnsReceiverTransfer extends TestAccumulationTransfer {
  /**
   * default constructor
   *
   * @param analysis the analysis
   */
  public TestAccumulationNoReturnsReceiverTransfer(CFAnalysis analysis) {
    super(analysis);
  }
}
