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

/**
 * This class provides methods used by the Non-Empty Checker as transfer functions for type rules
 * that cannot be expressed via simple pre- or post-conditional annotations.
 */
public class NonEmptyTransfer extends CFTransfer {

  /** A {@link ProcessingEnvironment} instance. */
  private final ProcessingEnvironment env;

  /** The {@code size()} method of the {@link java.util.Collection} interface. */
  private final ExecutableElement collectionSize;

  /** A {@link NonEmptyAnnotatedTypeFactory} instance. */
  private final NonEmptyAnnotatedTypeFactory aTypeFactory;

  public NonEmptyTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);

    this.env = analysis.getTypeFactory().getProcessingEnv();
    this.collectionSize = TreeUtils.getMethod("java.util.Collection", "size", 0, this.env);
    this.aTypeFactory = (NonEmptyAnnotatedTypeFactory) analysis.getTypeFactory();
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

    // A < B is equivalent to B > A
    refineGT(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());
    // This handles the case where n < container.size()
    refineGTE(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
      LessThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(n, in);

    // A <= B is equivalent to B > A
    refineGT(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());
    // This handles the case where n <= container.size()
    refineGTE(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThan(
      GreaterThanNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThan(n, in);
    refineGT(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(n, in);
    refineGTE(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    return result;
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() != n} and {@code container.size() != n}.
   *
   * <p>For example, the type of {@code container} in the "then" branch of a conditional statement
   * with the test {@code container.size() != n} where {@code n} is 0 should refine to
   * {@code @NonEmpty}.
   *
   * @param lhs the right-hand side of a not equal operator
   * @param rhs the left-hand side of a not equals operator
   * @param in the initial transfer result before refinement
   */
  private void refineNotEqual(Node lhs, Node rhs, TransferResult<CFValue, CFStore> in) {
    if (isSizeAccess(lhs) && rhs instanceof IntegerLiteralNode) {
      IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) rhs;
      if (integerLiteralNode.getValue() == 0) {
        JavaExpression receiver = getReceiver(lhs);
        in.getThenStore().insertValue(receiver, aTypeFactory.NON_EMPTY);
      }
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() > n}.
   *
   * <p>For example, the type of {@code container} in the "then" branch of a conditional statement
   * with the test {@code container.size() > n} where {@code n >= 0} should be refined to
   * {@code @NonEmpty}.
   *
   * @param lhs the left-hand side of a greater-than operation
   * @param rhs the right-hand side of a greater-than operation
   * @param store the abstract store to update
   */
  private void refineGT(Node lhs, Node rhs, CFStore store) {
    if (isSizeAccess(lhs) && rhs instanceof IntegerLiteralNode) {
      IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) rhs;
      if (integerLiteralNode.getValue() >= 0) {
        JavaExpression receiver = getReceiver(lhs);
        store.insertValue(receiver, aTypeFactory.NON_EMPTY);
      }
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() >= n}.
   *
   * <p>For example, the type of {@code container} in the "then" branch of a conditional statement
   * with the test {@code container.size() >= n} where {@code n > 0} should be refined to
   * {@code @NonEmpty}.
   *
   * @param lhs the left-hand side of a greater-than-or-equal operation
   * @param rhs the right-hand side of a greater-than-or-equal operation
   * @param store the abstract store to update
   */
  private void refineGTE(Node lhs, Node rhs, CFStore store) {
    if (isSizeAccess(lhs) && rhs instanceof IntegerLiteralNode) {
      IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) rhs;
      if (integerLiteralNode.getValue() > 0) {
        JavaExpression receiver = getReceiver(lhs);
        store.insertValue(receiver, aTypeFactory.NON_EMPTY);
      }
    }
  }

  /**
   * Return true if the given node is an instance of a method invocation node for {@code
   * Collection.size()}.
   *
   * @param possibleSizeAccess a node that may be a method call to the {@code size()} method in the
   * @return true if the node is a method call to Collection.size()
   */
  private boolean isSizeAccess(Node possibleSizeAccess) {
    return NodeUtils.isMethodInvocation(possibleSizeAccess, collectionSize, env);
  }

  /**
   * Return the receiver as a {@link JavaExpression} given a method invocation node.
   *
   * @param node an instance of a method access
   * @return the receiver as a {@link JavaExpression}
   */
  private JavaExpression getReceiver(Node node) {
    MethodAccessNode methodAccessNode = ((MethodInvocationNode) node).getTarget();
    return JavaExpression.fromNode(methodAccessNode.getReceiver());
  }
}
