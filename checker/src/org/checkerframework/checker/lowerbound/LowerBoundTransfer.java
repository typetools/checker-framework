package org.checkerframework.checker.lowerbound;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.lowerbound.qual.*;

import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;

import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.EqualToNode;

import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class LowerBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, LowerBoundTransfer> {
    protected LowerBoundAnalysis analysis;

    private final AnnotationMirror N1P, NN, POS, UNKNOWN;

    private LowerBoundAnnotatedTypeFactory atypeFactory;

    public LowerBoundTransfer(LowerBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        N1P = atypeFactory.getN1P();
        NN = atypeFactory.getNN();
        POS = atypeFactory.getPOS();
        UNKNOWN = atypeFactory.getUNKNOWN();
    }

    /** encodes what to do when we run into a greater-than node */
    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(GreaterThanNode node,
                                                             TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThan(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
            new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        /** do things here */
        greaterThanHelper(left, right, thenStore);
        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(GreaterThanOrEqualNode node,
                                                             TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
            new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        /** this makes sense because they have the same result in the chart. If I'm wrong about
            that, then we'd need to make something else to put here */
        equalToHelper(left, right, thenStore);
        return newResult;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(LessThanOrEqualNode node,
                                                             TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
            new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        /** call the inverse, because a LTE only gives us info for the else branch */
        greaterThanHelper(right, left, elseStore);
        return newResult;
    }
    
    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(LessThanNode node,
                                                             TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitLessThan(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
            new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        /** call the inverse, because a LT only gives us info for the else branch */
        /** nb if we change GreaterThanOrEqual we need to change this too */
        equalToHelper(right, left, elseStore);
        return newResult;
    }
    
    @Override
    public TransferResult<CFValue, CFStore>
        visitEqualTo(EqualToNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitEqualTo(node, in);
        Node left = node.getLeftOperand();
        Node right = node.getRightOperand();
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);

        /** have to call this in both directions since it only adjusts the first argument */
        /** trust me, this is the simpler way to do this */
        equalToHelper(left, right, thenStore);
        equalToHelper(right, left, thenStore);
        return newResult;
    }
    
    private void greaterThanHelper(Node left, Node right, CFStore store) {
        AnnotatedTypeMirror rightType = atypeFactory.getAnnotatedType(right.getTree());
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, left);
        if (rightType.hasAnnotation(N1P)) {
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
    /** if you're actually checking equality, you'd want to call this twice - once in each direction */
    private void equalToHelper(Node left, Node right, CFStore store) {
        AnnotatedTypeMirror rightType = atypeFactory.getAnnotatedType(right.getTree());
        AnnotatedTypeMirror leftType = atypeFactory.getAnnotatedType(left.getTree());
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
        if(leftType.hasAnnotation(NN)) {
            return;
        }
        if (rightType.hasAnnotation(N1P)) {
            store.insertValue(leftRec, N1P);
            return;
        }
        return;
    }
}
