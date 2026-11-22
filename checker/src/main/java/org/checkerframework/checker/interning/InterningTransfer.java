package org.checkerframework.checker.interning;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/** The transfer function for the Interning Checker. */
public class InterningTransfer extends CFTransfer {

  /**
   * True while {@link #visitCase} runs. Then, {@link #strengthenAnnotationOfEqualTo} does not
   * refine the switch selector.
   */
  private boolean inVisitCase = false;

  /**
   * Creates a new InterningTransfer.
   *
   * @param analysis an analysis for the Interning Checker
   */
  public InterningTransfer(CFAnalysis analysis) {
    super(analysis);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The Interning Checker does not refine the switch selector to the type of a case constant. A
   * switch compares its selector to a case constant by value (for a boxed primitive) or by {@code
   * equals()} (for a {@code String}), not by reference equality. So, the selector need not be the
   * same object as an interned case constant. The case has all other effects on the store, such as
   * the effects of code that might have side effects.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitCase(
      CaseNode n, TransferInput<CFValue, CFStore> in) {
    inVisitCase = true;
    try {
      return super.visitCase(n, in);
    } finally {
      inVisitCase = false;
    }
  }

  @Override
  protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
      TransferResult<CFValue, CFStore> res,
      Node firstNode,
      Node secondNode,
      @Nullable CFValue firstValue,
      @Nullable CFValue secondValue,
      boolean notEqualTo) {
    if (inVisitCase) {
      return res;
    }
    return super.strengthenAnnotationOfEqualTo(
        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
  }
}
