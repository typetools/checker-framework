package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.lowerbound.qual.*;

public class LowerBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, LowerBoundTransfer> {
    protected LowerBoundAnalysis analysis;

    private final AnnotationMirror N1P, NN, POS, UNKNOWN;

    public LowerBoundTransfer(LowerBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        N1P = ((LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory()).getN1P();
        NN = ((LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory()).getNN();
        POS = ((LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory()).getPOS();
        UNKNOWN = ((LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory()).getUNKNOWN();
    }
}
