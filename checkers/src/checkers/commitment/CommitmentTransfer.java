package checkers.commitment;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.Element;

import checkers.flow.analysis.ConditionalTransferResult;
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
import checkers.flow.cfg.node.MethodInvocationNode;

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
                    result.getRegularStore().addInitializedField(field);
                }
            }
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, CommitmentStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CommitmentStore> in) {
        TransferResult<CFValue, CommitmentStore> result = super
                .visitMethodInvocation(n, in);
        assert result instanceof ConditionalTransferResult;
        Set<Element> newlyInitializedFields = initializedFieldsAfterCall(n,
                (ConditionalTransferResult<CFValue, CommitmentStore>) result);
        if (newlyInitializedFields.size() > 0) {
            for (Element f : newlyInitializedFields) {
                result.getThenStore().addInitializedField(f);
                result.getElseStore().addInitializedField(f);
            }
        }
        return result;
    }

    /**
     * Returns the set of fields that can safely be considered initialized after
     * the method call {@code node}.
     *
     * @param result
     */
    protected Set<Element> initializedFieldsAfterCall(
            MethodInvocationNode node,
            ConditionalTransferResult<CFValue, CommitmentStore> result) {
        return Collections.emptySet();
    }
}
