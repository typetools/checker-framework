package org.checkerframework.checker.calledmethods;

import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import javax.lang.model.element.Name;

/** A transfer function that accumulates the names of methods called. */
public class CalledMethodsTransfer extends AccumulationTransfer {

    /**
     * Create a new CalledMethodsTransfer.
     *
     * @param analysis the analysis
     */
    public CalledMethodsTransfer(final CFAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);
        Node receiver = node.getTarget().getReceiver();
        if (receiver != null) {
            Name methodName = node.getTarget().getMethod().getSimpleName();
            String methodNameString =
                    ((CalledMethodsAnnotatedTypeFactory) atypeFactory)
                            .adjustMethodNameUsingValueChecker(methodName, node.getTree());
            accumulate(receiver, result, methodNameString);
        }
        return result;
    }
}
