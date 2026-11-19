package org.checkerframework.checker.interning;

import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/** The transfer function for the Interning Checker. */
public class InterningTransfer extends CFTransfer {

  /**
   * Creates a new InterningTransfer.
   *
   * @param analysis the analysis
   */
  public InterningTransfer(CFAnalysis analysis) {
    super(analysis);
  }

  /**
   * Does not refine the switch selector. A switch compares its selector to a case constant by value
   * (for a boxed primitive) or by {@code equals()} (for a {@code String}), not by reference
   * equality. So, the selector need not be the same object as an interned case constant.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitCase(
      CaseNode n, TransferInput<CFValue, CFStore> in) {
    return new RegularTransferResult<>(null, in.getRegularStore());
  }
}
