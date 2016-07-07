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
//import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
//import org.checkerframework.dataflow.cfg.node.LessThanNode;
//import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
//import org.checkerframework.dataflow.cfg.node.NotEqualNode;

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

    private void greaterThanHelper(Node left, Node right, CFStore store) {
        AnnotatedTypeMirror rightType = atypeFactory.getAnnotatedType(right.getTree());
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, left);
        if(rightType.hasAnnotation(N1P)) {
            store.insertValue(leftRec, NN);
            return;
        }
        if(rightType.hasAnnotation(NN)) {
            store.insertValue(leftRec, POS);
            return;
        }
        if(rightType.hasAnnotation(POS)) {
            store.insertValue(leftRec, POS);
            return;
        }
    }
}
