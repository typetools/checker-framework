package org.checkerframework.checker.lowerbound;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.lowerbound.qual.GTENegativeOne;
import org.checkerframework.checker.lowerbound.qual.LowerBoundUnknown;
import org.checkerframework.checker.lowerbound.qual.NonNegative;
import org.checkerframework.checker.lowerbound.qual.Positive;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
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
public class LowerBoundTransfer extends IndexAbstractTransfer {

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

    public LowerBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        aTypeFactory = (LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        // Initialize qualifiers.
        GTEN1 = aTypeFactory.GTEN1;
        NN = aTypeFactory.NN;
        POS = aTypeFactory.POS;
        UNKNOWN = aTypeFactory.UNKNOWN;
    }

    /**
     * Refines GTEN1 to NN if it is not equal to -1, and NN to Pos if it is not equal to 0.
     *
     * @param mLiteral a potential literal
     * @param otherNode the node on the other side of the ==/!=
     * @param otherAnno the annotation of the other side of the ==/!=
     */
    private void notEqualToValue(
            Node mLiteral, Node otherNode, AnnotationMirror otherAnno, CFStore store) {

        Long integerLiteralOrNull = aTypeFactory.getExactValueOrNullFromTree(mLiteral.getTree());

        if (integerLiteralOrNull == null) {
            return;
        }

        if (integerLiteralOrNull == 0) {
            if (AnnotationUtils.areSameByClass(otherAnno, NonNegative.class)) {
                Receiver rec = FlowExpressions.internalReprOf(aTypeFactory, otherNode);
                store.insertValue(rec, POS);
            }
        } else if (integerLiteralOrNull == -1) {
            if (AnnotationUtils.areSameByClass(otherAnno, GTENegativeOne.class)) {
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
        result =
                super.strengthenAnnotationOfEqualTo(
                        result, firstNode, secondNode, firstValue, secondValue, notEqualTo);

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, secondNode, firstNode);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        // There is also special processing to look
        // for literals on one side of the equals and a GTEN1 or NN on the other, so that
        // those types can be promoted in the branch where their values are not equal to certain
        // literals.
        CFStore notEqualsStore = notEqualTo ? rfi.thenStore : rfi.elseStore;
        notEqualToValue(rfi.left, rfi.right, rfi.rightAnno, notEqualsStore);
        notEqualToValue(rfi.right, rfi.left, rfi.leftAnno, notEqualsStore);

        return rfi.newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. Changes the type of left (the
     * greater one) to one closer to bottom than the type of right. Can't call the promote function
     * from the ATF directly because a new expression isn't introduced here - the modifications have
     * to be made to an existing one.
     */
    @Override
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {

        if (rightAnno == null || leftAnno == null) {
            return;
        }

        Receiver leftRec = FlowExpressions.internalReprOf(aTypeFactory, left);

        if (AnnotationUtils.areSame(rightAnno, GTEN1)) {
            store.insertValue(leftRec, NN);
            return;
        }
        if (AnnotationUtils.areSame(rightAnno, NN)) {
            store.insertValue(leftRec, POS);
            return;
        }
        if (AnnotationUtils.areSame(rightAnno, POS)) {
            store.insertValue(leftRec, POS);
            return;
        }
    }

    /**
     * Refines left to exactly the level of right, since in the worst case they're equal. Modifies
     * an existing type in the store, but has to be careful not to overwrite a more precise existing
     * type.
     */
    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {

        if (rightAnno == null || leftAnno == null) {
            return;
        }

        Receiver leftRec = FlowExpressions.internalReprOf(aTypeFactory, left);

        AnnotationMirror newLBType =
                aTypeFactory.getQualifierHierarchy().greatestLowerBound(rightAnno, leftAnno);

        store.insertValue(leftRec, newLBType);
    }
}
