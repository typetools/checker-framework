package checkers.commitment;

import javax.lang.model.element.Element;

import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.FlowExpressions.ThisReference;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractTransfer;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.node.AssignmentNode;

/**
 * A transfer function that extends {@link CFAbstractTransfer} and tracks
 * {@link CommitmentStore}s. In addition to the features of
 * {@link CFAbstractTransfer}, this transfer function also track which fields of
 * the current class ('self' receiver) have been initialized.
 * 
 * @author Stefan Heule
 * @see CommitmentStore
 * 
 * @param <T>
 *            The type of the transfer function.
 */
public class CommitmentTransfer<T extends CommitmentTransfer<T>> extends
        CFAbstractTransfer<CFValue, CommitmentStore, T> {

    public CommitmentTransfer(
            CFAbstractAnalysis<CFValue, CommitmentStore, T> analysis) {
        super(analysis);
        this.analysis = analysis;
    }

    @Override
    public TransferResult<CFValue, CommitmentStore> visitAssignment(
            AssignmentNode n, TransferInput<CFValue, CommitmentStore> in) {
        TransferResult<CFValue, CommitmentStore> result = super
                .visitAssignment(n, in);
        assert result instanceof RegularTransferResult;
        Receiver expr = FlowExpressions.internalReprOf(analysis.getFactory(),
                n.getTarget());

        // If this is an assignment to a field of 'this', then mark the field as
        // initialized.
        if (!expr.containsUnknown()) {
            if (expr instanceof FieldAccess) {
                FieldAccess fa = (FieldAccess) expr;
                if (fa.getReceiver() instanceof ThisReference) {
                    Element field = fa.getField();
                    in.getRegularStore().addInitializedField(field);
                }
            }
        }
        return result;
    }
}
