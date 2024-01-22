package org.checkerframework.common.initializedfields;

import org.checkerframework.common.accumulation.AccumulationAnalysis;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;

/** Accumulates the names of fields that are initialized. */
public class InitializedFieldsTransfer extends AccumulationTransfer {

  /**
   * Create an InitializedFieldsTransfer.
   *
   * @param analysis the analysis
   */
  public InitializedFieldsTransfer(AccumulationAnalysis analysis) {
    super(analysis);
  }

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitAssignment(
      AssignmentNode node, TransferInput<AccumulationValue, AccumulationStore> input) {
    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitAssignment(node, input);
    Node lhs = node.getTarget();
    if (lhs instanceof FieldAccessNode) {
      FieldAccessNode fieldAccess = (FieldAccessNode) lhs;
      accumulate(fieldAccess.getReceiver(), result, fieldAccess.getFieldName());
    }
    return result;
  }
}
