package org.checkerframework.checker.index.inequality;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

public class LessThanTransfer extends IndexAbstractTransfer {

    public LessThanTransfer(CFAnalysis analysis) {
        super(analysis);
    }

    @Override
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        // left > right so right is less than left
        // Refine right to @LessThan("left")
        Receiver leftRec = FlowExpressions.internalReprOf(factory, left);
        if (leftRec != null && leftRec.isUnmodifiableByOtherCode()) {
            List<String> lessThanExpressions =
                    LessThanAnnotatedTypeFactory.getLessThanExpressions(rightAnno);
            if (lessThanExpressions == null) {
                // right is already bottom, nothing to refine.
                return;
            }
            lessThanExpressions.add(leftRec.toString());
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            store.insertValue(rightRec, factory.createLessThanQualifier(lessThanExpressions));
        }

        // TODO: Could add transitive here:
        // other > left and left > right then other > right
    }

    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        // left >= right so right is less than left
        // Refine right to @LessThan("left + 1")

        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        // left > right so right is less than left
        // Refine right to @LessThan("left")
        Receiver leftRec = FlowExpressions.internalReprOf(factory, left);
        if (leftRec != null && leftRec.isUnmodifiableByOtherCode()) {
            List<String> lessThanExpressions =
                    LessThanAnnotatedTypeFactory.getLessThanExpressions(rightAnno);
            if (lessThanExpressions == null) {
                // right is already bottom, nothing to refine.
                return;
            }
            lessThanExpressions.add(leftRec.toString() + " + 1");
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
            store.insertValue(rightRec, factory.createLessThanQualifier(lessThanExpressions));
        }
    }

    //    @Override
    //    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(NumericalSubtractionNode node,
    //            TransferInput<CFValue, CFStore> in) {
    //        TransferResult<CFValue, CFStore> result = super.visitNumericalSubtraction(node, in);
    //        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);
    //
    //
    //
    //
    //        return createNewResult(result, newAnno);
    //    }
    //
    //    /**
    //     * Create a new transfer result based on the original result and the new annotation.
    //     *
    //     * @param result the original result
    //     * @param resultAnno the new annotation
    //     * @return the new transfer result
    //     */
    //    private TransferResult<CFValue, CFStore> createNewResult(
    //            TransferResult<CFValue, CFStore> result, AnnotationMirror resultAnno) {
    //        CFValue newResultValue =
    //                analysis.createSingleAnnotationValue(
    //                        resultAnno, result.getResultValue().getUnderlyingType());
    //        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    //    }
}
