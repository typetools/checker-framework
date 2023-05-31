package org.checkerframework.framework.testchecker.testaccumulation;

import org.checkerframework.common.accumulation.AccumulationAnalysis;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;

/** A basic transfer function that accumulates the names of methods called. */
public class TestAccumulationTransfer extends AccumulationTransfer {

  /**
   * default constructor
   *
   * @param analysis the analysis
   */
  public TestAccumulationTransfer(AccumulationAnalysis analysis) {
    super(analysis);
  }

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<AccumulationValue, AccumulationStore> input) {
    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitMethodInvocation(node, input);
    Node receiver = node.getTarget().getReceiver();
    if (receiver != null) {
      String methodName = node.getTarget().getMethod().getSimpleName().toString();
      accumulate(receiver, result, methodName);
    }
    return result;
  }
}
