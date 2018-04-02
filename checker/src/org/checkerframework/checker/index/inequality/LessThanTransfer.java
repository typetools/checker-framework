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

/**
 * Implements 2 refinement rules:
 *
 * <ul>
 *   <li>1. if left &gt; right, right has type {@code @LessThan("left")}
 *   <li>2. if left &ge; right, right has type {@code @LessThan("left + 1")}
 * </ul>
 *
 * These are implemented generally, so they also apply to e.g. &lt; and &le; comparisons.
 */
public class LessThanTransfer extends IndexAbstractTransfer {

    public LessThanTransfer(CFAnalysis analysis) {
        super(analysis);
    }

    /** Case 1. */
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
        // left > right so right < left
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
    }

    /** Case 2. */
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
}
