package org.checkerframework.checker.lowerbound;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.lowerbound.qual.GTENegativeOne;
import org.checkerframework.checker.lowerbound.qual.LowerBoundUnknown;
import org.checkerframework.checker.lowerbound.qual.NonNegative;
import org.checkerframework.checker.lowerbound.qual.Positive;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Implements dataflow refinement rules based on tests: &lt;, &gt;, ==, and their derivatives.
 *
 * <p>&gt;, &lt;, &ge;, &le;, ==, and != nodes are represented as combinations of &gt; and &ge;
 * (e.g. == is &ge; in both directions in the then branch), and implement refinements based on these
 * decompositions.
 *
 * <pre>
 * Refinement/transfer rules for conditionals:
 *
 * There are two "primitives":
 *
 * x &gt; y, which implies things about x based on y's type:
 *
 * y has type:    implies x has type:
 *  gte-1                nn
 *  nn                   pos
 *  pos                  pos
 *
 * and x &ge; y:
 *
 * y has type:    implies x has type:
 *  gte-1                gte-1
 *  nn                   nn
 *  pos                  pos
 *
 * These two "building blocks" can be combined to make all
 * other conditional expressions:
 *
 * EXPR             THEN          ELSE
 * x &gt; y            x &gt; y         y &ge; x
 * x &ge; y           x &ge; y        y &gt; x
 * x &lt; y            y &gt; x         x &ge; y
 * x &le; y           y &ge; x        x &gt; y
 *
 * Or, more formally:
 *
 * EXPR        THEN                                        ELSE
 * x &gt; y       x_refined = GLB(x_orig, promote(y))         y_refined = GLB(y_orig, x)
 * x &ge; y      x_refined = GLB(x_orig, y)                  y_refined = GLB(y_orig, promote(x))
 * x &lt; y       y_refined = GLB(y_orig, promote(x))         x_refined = GLB(x_orig, y)
 * x &le; y      y_refined = GLB(y_orig, x)                  x_refined = GLB(x_orig, promote(y))
 *
 * where GLB is the greatest lower bound and promote is the increment
 * function on types (or, equivalently, the function specified by the "x
 * &gt; y" information above).
 *
 * There's also ==, which is a special case. Only the THEN
 * branch is refined:
 *
 * EXPR             THEN                   ELSE
 * x == y           x &ge; y &amp;&amp; y &ge; x       nothing known
 *
 * or, more formally:
 *
 * EXPR            THEN                                    ELSE
 * x == y          x_refined = GLB(x_orig, y_orig)         nothing known
 *                y_refined = GLB(x_orig, y_orig)
 *
 * finally, not equal:
 *
 * EXPR             THEN                   ELSE
 * x != y           nothing known          x &ge; y &amp;&amp; y &ge; x
 *
 * more formally:
 *
 * EXPR            THEN               ELSE
 * x != y          nothing known      x_refined = GLB(x_orig, y_orig)
 *                                   y_refined = GLB(x_orig, y_orig)
 *
 * </pre>
 */
public class LowerBoundTransfer extends CFTransfer {

    /** The canonical {@link GTENegativeOne} annotation. */
    public final AnnotationMirror GTEN1;
    /** The canonical {@link NonNegative} annotation. */
    public final AnnotationMirror NN;
    /** The canonical {@link Positive} annotation. */
    public final AnnotationMirror POS;
    /** The canonical {@link LowerBoundUnknown} annotation. */
    public final AnnotationMirror UNKNOWN;

    // The ATF (Annotated Type Factory).
    private LowerBoundAnnotatedTypeFactory aTypeFactory;

    private CFAnalysis analysis;

    public LowerBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        aTypeFactory = (LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        // Initialize qualifiers.
        GTEN1 = aTypeFactory.GTEN1;
        NN = aTypeFactory.NN;
        POS = aTypeFactory.POS;
        UNKNOWN = aTypeFactory.UNKNOWN;
    }

    /**
     * This struct contains all of the information that the refinement functions need. It's called
     * by each node function (i.e. greater than node, less than node, etc.) and then the results are
     * passed to the refinement function in whatever order is appropriate for that node. It's
     * constructor contains all of its logic.
     */
    private class RefinementInfo {
        public Node left, right;
        public Set<AnnotationMirror> leftType, rightType;
        public CFStore thenStore, elseStore;
        public ConditionalTransferResult<CFValue, CFStore> newResult;

        public RefinementInfo(
                TransferResult<CFValue, CFStore> result, CFAnalysis analysis, Node r, Node l) {
            right = r;
            left = l;

            if (analysis.getValue(right) == null || analysis.getValue(left) == null) {
                leftType = null;
                rightType = null;
                newResult =
                        new ConditionalTransferResult<>(
                                result.getResultValue(), thenStore, elseStore);
            } else {

                rightType = analysis.getValue(right).getAnnotations();
                leftType = analysis.getValue(left).getAnnotations();

                thenStore = result.getRegularStore();
                elseStore = thenStore.copy();

                newResult =
                        new ConditionalTransferResult<>(
                                result.getResultValue(), thenStore, elseStore);
            }
        }
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThan(node, in);
        RefinementInfo rfi =
                new RefinementInfo(result, analysis, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, analysis, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, analysis, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThan(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, analysis, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A < is just a flipped >.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    /**
     * Refines GTEN1 to NN if it's compared directly to -1, and NN to Pos if it's compared to 0.
     *
     * @param mLiteral a potential literal
     * @param otherNode the node on the other side of the ==/!=
     * @param otherType the type of the other side of the ==/!=
     */
    private void handleRelevantLiteralForEquals(
            Node mLiteral, Node otherNode, Set<AnnotationMirror> otherType, CFStore store) {

        Long integerLiteralOrNull = aTypeFactory.getExactValueOrNullFromTree(mLiteral.getTree());

        if (integerLiteralOrNull == null) {
            return;
        }

        if (integerLiteralOrNull == 0) {
            if (AnnotationUtils.containsSameByClass(otherType, NonNegative.class)) {
                Receiver rec = FlowExpressions.internalReprOf(aTypeFactory, otherNode);
                store.insertValue(rec, POS);
            }
        } else if (integerLiteralOrNull == -1) {
            if (AnnotationUtils.containsSameByClass(otherType, GTENegativeOne.class)) {
                Receiver rec = FlowExpressions.internalReprOf(aTypeFactory, otherNode);
                store.insertValue(rec, NN);
            }
        }
    }

    /** Implements the transfer rules for both equal nodes and not-equals nodes. */
    @Override
    protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CFStore> result,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {

        //  In an ==, refinements occur in the then branch (i.e. when they are,
        // actually, equal). In that case, they are refined to the more
        // precise of the two types, which is accomplished by refining each as if it were
        // greater than or equal to the other. There is also special processing to look
        // for literals on one side of the equals and a GTEN1 or NN on the other, so that
        // those types can be promoted in the else branch if compared against the appropriate
        // single literal. != is equivalent to == and implemented the same way, but the refinements occur in
        // the other branch (i.e. when they are !equal).

        if (notEqualTo) {
            // Process != first.

            RefinementInfo rfi = new RefinementInfo(result, analysis, secondNode, firstNode);

            handleRelevantLiteralForEquals(rfi.left, rfi.right, rfi.rightType, rfi.thenStore);
            handleRelevantLiteralForEquals(rfi.right, rfi.left, rfi.leftType, rfi.thenStore);

            refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
            refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
            return rfi.newResult;
        } else {
            // Process ==.

            RefinementInfo rfi = new RefinementInfo(result, analysis, secondNode, firstNode);

            handleRelevantLiteralForEquals(rfi.left, rfi.right, rfi.rightType, rfi.elseStore);
            handleRelevantLiteralForEquals(rfi.right, rfi.left, rfi.leftType, rfi.elseStore);

            refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);
            refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
            return rfi.newResult;
        }
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. Changes the type of left (the
     * greater one) to one closer to bottom than the type of right. Can't call the promote function
     * from the ATF directly because a new expression isn't introduced here - the modifications have
     * to be made to an existing one.
     */
    private void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {

        if (rightType == null || leftType == null) {
            return;
        }

        Receiver leftRec = FlowExpressions.internalReprOf(aTypeFactory, left);

        if (AnnotationUtils.containsSame(rightType, GTEN1)) {
            store.insertValue(leftRec, NN);
            return;
        }
        if (AnnotationUtils.containsSame(rightType, NN)) {
            store.insertValue(leftRec, POS);
            return;
        }
        if (AnnotationUtils.containsSame(rightType, POS)) {
            store.insertValue(leftRec, POS);
            return;
        }
    }

    /**
     * Refines left to exactly the level of right, since in the worst case they're equal. Modifies
     * an existing type in the store, but has to be careful not to overwrite a more precise existing
     * type.
     */
    private void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            CFStore store) {

        if (rightType == null || leftType == null) {
            return;
        }

        Receiver leftRec = FlowExpressions.internalReprOf(aTypeFactory, left);

        AnnotationMirror rightLBType =
                aTypeFactory.getQualifierHierarchy().findAnnotationInHierarchy(rightType, UNKNOWN);
        AnnotationMirror leftLBType =
                aTypeFactory.getQualifierHierarchy().findAnnotationInHierarchy(rightType, UNKNOWN);

        AnnotationMirror newLBType =
                aTypeFactory.getQualifierHierarchy().greatestLowerBound(rightLBType, leftLBType);

        if (rightLBType != null && leftLBType != null && newLBType != null) {
            store.insertValue(leftRec, newLBType);
        }
    }
}
