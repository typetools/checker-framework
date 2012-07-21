package checkers.nonnull;

import javax.lang.model.element.AnnotationMirror;

import checkers.commitment.CommitmentStore;
import checkers.commitment.CommitmentTransfer;
import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.NullLiteralNode;
import checkers.nonnull.quals.NonNull;

/**
 * Transfer function for the non-null type system. Performs the following
 * refinements:
 * <ol>
 * <li>After the call to a constructor ("this()" call), all non-null fields of
 * the current class can safely be considered initialized.
 * <li>TODO: After a method call with a postcondition that ensures a field to be
 * non-null, that field can safely be considered initialized.
 * <li>All non-null fields with an initializer can be considered initialized.
 * <li>After the call to a super constructor ("super()" call), all non-null
 * fields of the super class can safely be considered initialized.
 * </ol>
 *
 * @author Stefan Heule
 */
public class NonNullTransfer extends CommitmentTransfer<NonNullTransfer> {

    /** Type-specific version of super.analysis. */
    protected final NonNullAnalysis analysis;

    public NonNullTransfer(NonNullAnalysis analysis, NonNullChecker checker) {
        super(analysis, checker);
        this.analysis = analysis;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Furthermore, this method refines the type to {@code NonNull} for the
     * appropriate branch if an expression is compared to the {@code null}
     * literal.
     */
    @Override
    protected TransferResult<CFValue, CommitmentStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CommitmentStore> res, Node firstNode,
            Node secondNode, CFValue firstValue, CFValue secondValue,
            boolean notEqualTo) {
        res = super.strengthenAnnotationOfEqualTo(res, firstNode, secondNode,
                firstValue, secondValue, notEqualTo);
        if (firstNode instanceof NullLiteralNode) {
            Receiver secondInternal = FlowExpressions.internalReprOf(
                    analysis.getFactory(), secondNode);
            if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                CommitmentStore thenStore = res.getThenStore();
                CommitmentStore elseStore = res.getElseStore();
                AnnotationMirror nonNull = analysis.getFactory()
                        .annotationFromClass(NonNull.class);
                if (notEqualTo) {
                    thenStore.insertValue(secondInternal, nonNull);
                } else {
                    elseStore.insertValue(secondInternal, nonNull);
                }
                return new ConditionalTransferResult<>(res.getResultValue(),
                        thenStore, elseStore);
            }
        }
        return res;
    }
}
