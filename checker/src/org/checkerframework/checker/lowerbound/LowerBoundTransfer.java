package org.checkerframework.checker.lowerbound;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.lowerbound.qual.*;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 *  Implements the refinement rules described in lowerbound_rules.txt.
 *  In particular, implements data flow refinements based on tests: &lt;,
 *  &gt;, ==, and their derivatives.
 * <p>
 *
 *  We represent &gt;, &lt;, &ge;, &le;, ==, and != nodes as combinations
 *  of &gt; and &ge; (e.g. == is &ge; in both directions in the then
 *  branch), and implement refinements based on these decompositions.
 */
public class LowerBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, LowerBoundTransfer> {

    protected LowerBoundAnalysis analysis;

    public final AnnotationMirror GTEN1;
    /** The canonical @Negative annotation. */
    public final AnnotationMirror NN;
    /** The canonical @Positive annotation. */
    public final AnnotationMirror POS;
    /** The canonical @LowerBoundUnknown annotation. */
    public final AnnotationMirror UNKNOWN;

    // The ATF (Annotated Type Factory).
    private LowerBoundAnnotatedTypeFactory atypeFactory;

    public LowerBoundTransfer(LowerBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        // Initialize qualifiers.
        GTEN1 = atypeFactory.GTEN1;
        NN = atypeFactory.NN;
        POS = atypeFactory.POS;
        UNKNOWN = atypeFactory.UNKNOWN;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThan(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        AnnotatedTypeMirror rightType = in.getValueOfSubNode(node.getRightOperand()).getType();
        AnnotatedTypeMirror leftType = in.getValueOfSubNode(node.getLeftOperand()).getType();
        // Refine the then branch.
        refineGT(left, leftType, right, rightType, thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(right, rightType, left, leftType, elseStore);

        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        AnnotatedTypeMirror rightType = in.getValueOfSubNode(node.getRightOperand()).getType();
        AnnotatedTypeMirror leftType = in.getValueOfSubNode(node.getLeftOperand()).getType();

        // Refine the then branch.
        refineGTE(left, leftType, right, rightType, thenStore);

        // Refine the else branch.
        refineGT(right, rightType, left, leftType, elseStore);

        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        AnnotatedTypeMirror rightType = in.getValueOfSubNode(node.getRightOperand()).getType();
        AnnotatedTypeMirror leftType = in.getValueOfSubNode(node.getLeftOperand()).getType();

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(right, rightType, left, leftType, thenStore);

        // Refine the else branch.
        refineGT(left, leftType, right, rightType, elseStore);
        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThan(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        AnnotatedTypeMirror rightType = in.getValueOfSubNode(node.getRightOperand()).getType();
        AnnotatedTypeMirror leftType = in.getValueOfSubNode(node.getLeftOperand()).getType();

        // Refine the then branch. A < is just a flipped >.
        refineGT(right, rightType, left, leftType, thenStore);

        // Refine the else branch.
        refineGTE(left, leftType, right, rightType, elseStore);
        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitEqualTo(
            EqualToNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitEqualTo(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        AnnotatedTypeMirror rightType = in.getValueOfSubNode(node.getRightOperand()).getType();
        AnnotatedTypeMirror leftType = in.getValueOfSubNode(node.getLeftOperand()).getType();

        /*  In an ==, we only can make conclusions about the then
         *  branch (i.e. when they are, actually, equal). In that
         *  case, we essentially want to refine them to the more
         *  precise of the two types, which we accomplish by refining
         *  each as if it were greater than or equal to the other.
         */
        refineGTE(left, leftType, right, rightType, thenStore);
        refineGTE(right, rightType, left, leftType, thenStore);
        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(
            NotEqualNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitNotEqual(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        AnnotatedTypeMirror rightType = in.getValueOfSubNode(node.getRightOperand()).getType();
        AnnotatedTypeMirror leftType = in.getValueOfSubNode(node.getLeftOperand()).getType();

        /* != is equivalent to == and implemented the same way, but we
         * !have information about the else branch (i.e. when they are
         * !equal).
         */
        refineGTE(left, leftType, right, rightType, elseStore);
        refineGTE(right, rightType, left, leftType, elseStore);
        return newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test.
     * Effectively works by promoting the type of left (the greater
     * one) to one higher than the type of right. Can't call the promote
     * function from the ATF directly because we're not introducing a
     * new expression here - we have to modify an existing one.
     */
    private void refineGT(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, left);
        /* We don't want to overwrite a more precise type, so we don't modify
         * the left's type if it's already known to be positive.
         */
        if (rightType.hasAnnotation(GTEN1) && !leftType.hasAnnotation(POS)) {
            store.insertValue(leftRec, NN);
            return;
        }
        if (rightType.hasAnnotation(NN)) {
            store.insertValue(leftRec, POS);
            return;
        }
        if (rightType.hasAnnotation(POS)) {
            store.insertValue(leftRec, POS);
            return;
        }
    }

    /**
     * Elevates left to exactly the level of right, since in the
     * worst case they're equal. Modifies an existing type in the
     * store.
     */
    private void refineGTE(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, left);
        /* We are effectively calling GLB(right, left) here, but we're
         * doing it manually because of the need to modify things
         * directly.
         */
        if (rightType.hasAnnotation(POS)) {
            store.insertValue(leftRec, POS);
            return;
        }
        if (leftType.hasAnnotation(POS)) {
            return;
        }
        if (rightType.hasAnnotation(NN)) {
            store.insertValue(leftRec, NN);
            return;
        }
        if (leftType.hasAnnotation(NN)) {
            return;
        }
        if (rightType.hasAnnotation(GTEN1)) {
            store.insertValue(leftRec, GTEN1);
            return;
        }
        return;
    }
}
