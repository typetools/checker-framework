package org.checkerframework.checker.objectconstruction;

import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/** A basic transfer function that accumulates the names of methods called. */
public class ObjectConstructionTransfer extends AccumulationTransfer {

    /** The type factory. */
    private final ObjectConstructionAnnotatedTypeFactory atypeFactory;

    /**
     * default constructor
     *
     * @param analysis the analysis
     */
    public ObjectConstructionTransfer(final CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (ObjectConstructionAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);
        String methodName = node.getTarget().getMethod().getSimpleName().toString();
        methodName = atypeFactory.adjustMethodNameUsingValueChecker(methodName, node.getTree());
        Node receiver = node.getTarget().getReceiver();
        if (!"<init>".equals(methodName) && receiver != null) {
            accumulate(receiver, result, methodName);
        }
        return result;
    }
}
