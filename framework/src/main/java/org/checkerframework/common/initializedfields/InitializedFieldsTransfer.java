package org.checkerframework.common.initializedfields;

import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/** Accumulates the names of fields that are initialized. */
public class InitializedFieldsTransfer extends AccumulationTransfer {

    /**
     * Create an InitializedFieldsTransfer.
     *
     * @param analysis the analysis
     */
    public InitializedFieldsTransfer(final CFAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            final AssignmentNode node, final TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, input);
        Node lhs = node.getTarget();
        if (lhs instanceof FieldAccessNode) {
            FieldAccessNode fieldAccess = (FieldAccessNode) lhs;
            accumulate(fieldAccess.getReceiver(), result, fieldAccess.getFieldName());
        }
        return result;
    }
}
