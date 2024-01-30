package org.checkerframework.checker.nonempty;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;

public class NonEmptyTransfer extends CFTransfer {

  private final ExecutableElement collectionSize;
  private final ProcessingEnvironment env;
  private final NonEmptyAnnotatedTypeFactory aTypeFactory;

  public NonEmptyTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);

    this.env = analysis.getTypeFactory().getProcessingEnv();
    this.aTypeFactory = (NonEmptyAnnotatedTypeFactory) analysis.getTypeFactory();
    this.collectionSize = TreeUtils.getMethod("java.util.Collection", "size", 0, this.env);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitNotEqual(
      NotEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitNotEqual(n, in);
    refineNotEqual(n.getLeftOperand(), n.getRightOperand(), result);
    refineNotEqual(n.getRightOperand(), n.getLeftOperand(), result);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThan(n, in);
    refineGT(n.getRightOperand(), n.getLeftOperand(), result);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
      LessThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(n, in);
    refineGTE(n.getRightOperand(), n.getLeftOperand(), result);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThan(
      GreaterThanNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThan(n, in);
    refineGT(n.getLeftOperand(), n.getRightOperand(), result);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(n, in);
    refineGTE(n.getLeftOperand(), n.getRightOperand(), result);
    return result;
  }

  private TransferResult<CFValue, CFStore> refineNotEqual(
      Node lhs, Node rhs, TransferResult<CFValue, CFStore> in) {
    if (!isSizeAccess(lhs)) {
      return in;
    }
    if (!(rhs instanceof IntegerLiteralNode)) {
      return in;
    }
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) rhs;
    if (integerLiteralNode.getValue() == 0) {
      // Update the `then` store to have @NonEmpty for the receiver of java.util.Collection.size;
      JavaExpression receiver = getReceiver(lhs);
      in.getThenStore().insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
    return in;
  }

  private TransferResult<CFValue, CFStore> refineGT(
      Node lhs, Node rhs, TransferResult<CFValue, CFStore> in) {
    if (!isSizeAccess(lhs)) {
      return in;
    }
    if (!(rhs instanceof IntegerLiteralNode)) {
      return in;
    }

    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) rhs;
    if (integerLiteralNode.getValue() >= 0) {
      // Update the `then` store to have @NonEmpty for the receiver of java.util.Collection.size;
      JavaExpression receiver = getReceiver(lhs);
      in.getThenStore().insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
    return in;
  }

  private TransferResult<CFValue, CFStore> refineGTE(
      Node lhs, Node rhs, TransferResult<CFValue, CFStore> in) {
    if (!isSizeAccess(lhs)) {
      return in;
    }
    if (!(rhs instanceof IntegerLiteralNode)) {
      return in;
    }

    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) rhs;
    if (integerLiteralNode.getValue() > 0) {
      // Update the `then` store to have @NonEmpty for the receiver of java.util.Collection.size;
      JavaExpression receiver = getReceiver(lhs);
      in.getThenStore().insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
    return in;
  }

  /**
   * Given a node that is a possible call to Collection.size(), return true if and only if this is
   * the case.
   *
   * @param possibleSizeAccess a node that may be a method call to Collection.size()
   * @return true iff the node is a method call to Collection.size()
   */
  private boolean isSizeAccess(Node possibleSizeAccess) {
    return NodeUtils.isMethodInvocation(possibleSizeAccess, collectionSize, env);
  }

  private JavaExpression getReceiver(Node sizeAccessNode) {
    MethodAccessNode methodAccessNode = ((MethodInvocationNode) sizeAccessNode).getTarget();
    return JavaExpression.fromNode(methodAccessNode.getReceiver());
  }
}
