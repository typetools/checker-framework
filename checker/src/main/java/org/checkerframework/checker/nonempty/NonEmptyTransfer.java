package org.checkerframework.checker.nonempty;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;

public class NonEmptyTransfer extends CFTransfer {

  private final ExecutableElement collectionSize;
  private final ProcessingEnvironment env;

  public NonEmptyTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);

    this.env = analysis.getTypeFactory().getProcessingEnv();
    this.collectionSize = TreeUtils.getMethod("java.util.Collection", "size", 0, this.env);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThan(
      GreaterThanNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThan(n, in);
    handleContainerSizeComparison(n.getLeftOperand(), n.getRightOperand(), result);
    return result; // stub
  }

  private TransferResult<CFValue, CFStore> handleContainerSizeComparison(
      Node possibleCollectionSize, Node possibleConstant, TransferResult<CFValue, CFStore> in) {
    if (!(possibleCollectionSize instanceof MethodInvocationNode)) {
      return in;
    }
    if (!(possibleConstant instanceof IntegerLiteralNode)) {
      return in;
    }

    if (isSizeAccess(possibleCollectionSize)) {
      IntegerLiteralNode comparedValue = (IntegerLiteralNode) possibleConstant;
      if (comparedValue.getValue() > 0) {
        // Update the `then` store to have @NonEmpty for the receiver of java.util.Collection.size;
      }
    }
    return in;
  }

  private boolean isSizeAccess(Node possibleSizeAccess) {
    return NodeUtils.isMethodInvocation(possibleSizeAccess, collectionSize, env);
  }
}
