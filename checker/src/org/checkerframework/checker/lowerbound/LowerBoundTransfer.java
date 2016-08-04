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

public class LowerBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, LowerBoundTransfer> {

    protected LowerBoundAnalysis analysis;

    // we'll pull these from our ATF
    private final AnnotationMirror GTEN1, NN, POS, UNKNOWN;

    // this is the ATF
    private LowerBoundAnnotatedTypeFactory atypeFactory;

    public LowerBoundTransfer(LowerBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        GTEN1 = atypeFactory.GTEN1;
        NN = atypeFactory.NN;
        POS = atypeFactory.POS;
        UNKNOWN = atypeFactory.UNKNOWN;
    }

    /** encodes what to do when we run into a greater-than node */
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
        /** do things here */
        refineGT(left, leftType, right, rightType, thenStore);

        /** inverse */
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

        /** this makes sense because they have the same result in the chart. If I'm wrong about
         * that, then we'd need to make something else to put here */
        refineGTE(left, leftType, right, rightType, thenStore);

        /** call the inverse */
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

        /** equivalent to a flipped GTE */
        refineGTE(right, rightType, left, leftType, thenStore);

        /** call the inverse */
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

        /** x < y ~ y > x */
        refineGT(right, rightType, left, leftType, thenStore);

        /** inverse */
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

        /** have to call this in both directions since it only adjusts the first argument */
        /** trust me, this is the simpler way to do this */
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

        /** have to call this in both directions since it only adjusts the first argument */
        /** trust me, this is the simpler way to do this */
        refineGTE(left, leftType, right, rightType, elseStore);
        refineGTE(right, rightType, left, leftType, elseStore);
        return newResult;
    }

    private void refineGT(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, left);
        if (rightType.hasAnnotation(GTEN1)) {
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

    /** this works by elevating left to the level of right, basically */
    /** to check equality, you'd want to call this twice - once in each direction */
    private void refineGTE(
            Node left,
            AnnotatedTypeMirror leftType,
            Node right,
            AnnotatedTypeMirror rightType,
            CFStore store) {
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, left);
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
