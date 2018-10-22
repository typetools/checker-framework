package org.checkerframework.checker.index.inequality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * Implements 3 refinement rules:
 *
 * <ul>
 *   <li>1. if left &gt; right, right has type {@code @LessThan("left")}
 *   <li>2. if left &ge; right, right has type {@code @LessThan("left + 1")}
 *   <li>3. if {@code 0 < right}, {@code left - right} has type {@code @LessThan("left")}
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

    /** Case 3. */
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> in) {
        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        Receiver leftRec = FlowExpressions.internalReprOf(factory, n.getLeftOperand());
        if (leftRec != null && leftRec.isUnmodifiableByOtherCode()) {
            ValueAnnotatedTypeFactory valueFactory = factory.getValueAnnotatedTypeFactory();
            Long right = IndexUtil.getMinValue(n.getRightOperand().getTree(), valueFactory);
            if (right != null && 0 < right) {
                // left - right < left iff 0 < right
                List<String> expressions = getLessThanExpressions(n.getLeftOperand());
                if (expressions == null) {
                    expressions = new ArrayList<>();
                }
                expressions.add(leftRec.toString());
                AnnotationMirror refine = factory.createLessThanQualifier(expressions);
                CFValue value = analysis.createSingleAnnotationValue(refine, n.getType());
                CFStore info = in.getRegularStore();
                return new RegularTransferResult<>(finishValue(value, info), info);
            }
        }
        return super.visitNumericalSubtraction(n, in);
    }

    /** Return the expressions that {@code node} are less than. */
    private List<String> getLessThanExpressions(Node node) {
        Set<AnnotationMirror> s = analysis.getValue(node).getAnnotations();
        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        if (s != null && !s.isEmpty()) {
            return LessThanAnnotatedTypeFactory.getLessThanExpressions(
                    factory.getQualifierHierarchy().findAnnotationInHierarchy(s, factory.UNKNOWN));
        } else {
            return Collections.emptyList();
        }
    }
}
