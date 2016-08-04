package org.checkerframework.checker.upperbound;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.upperbound.qual.*;
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

public class UpperBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, UpperBoundTransfer> {
    protected UpperBoundAnalysis analysis;

    private final AnnotationMirror LTL, EL, LTEL, UNKNOWN;

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    public UpperBoundTransfer(UpperBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        LTL = UpperBoundAnnotatedTypeFactory.LTL;
        EL = UpperBoundAnnotatedTypeFactory.EL;
        LTEL = UpperBoundAnnotatedTypeFactory.LTEL;
        UNKNOWN = UpperBoundAnnotatedTypeFactory.UNKNOWN;
    }
}
