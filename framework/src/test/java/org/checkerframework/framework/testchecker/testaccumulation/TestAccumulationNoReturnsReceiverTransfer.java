package org.checkerframework.framework.testchecker.testaccumulation;

import org.checkerframework.common.accumulation.AccumulationAnalysis;

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
  public TestAccumulationNoReturnsReceiverTransfer(AccumulationAnalysis analysis) {
    super(analysis);
  }
}
