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
 * <li>After an expression is compared with the {@code null} literal, then that
 * expression can safely be considered {@link NonNull} if the result of the
 * comparison is false.
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
     * literal (listed as case 1 in the class description).
     */
    @Override
    protected TransferResult<CFValue, CommitmentStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CommitmentStore> res, Node firstNode,
            Node secondNode, CFValue firstValue, CFValue secondValue,
            boolean notEqualTo) {
        res = super.strengthenAnnotationOfEqualTo(res, firstNode, secondNode,
                firstValue, secondValue, notEqualTo);
        // Case 1:
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
