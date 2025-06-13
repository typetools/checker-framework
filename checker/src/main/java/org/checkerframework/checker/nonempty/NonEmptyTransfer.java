package org.checkerframework.checker.nonempty;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
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
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class implements type rules that cannot be expressed via pre- or post-condition annotations.
 */
public class NonEmptyTransfer extends CFTransfer {

  /** A {@link ProcessingEnvironment} instance. */
  private final ProcessingEnvironment env;

  /** A {@link NonEmptyAnnotatedTypeFactory} instance. */
  protected final NonEmptyAnnotatedTypeFactory aTypeFactory;

  /** The {@link java.util.Collection#size()} method. */
  private final ExecutableElement collectionSize;

  /** The {@link java.util.Map#size()} method. */
  private final ExecutableElement mapSize;

  /** The {@link java.util.List#indexOf(Object)} method. */
  private final ExecutableElement listIndexOf;

  /**
   * Create a new {@link NonEmptyTransfer}.
   *
   * @param analysis the analysis for this transfer function
   */
  public NonEmptyTransfer(CFAnalysis analysis) {
    super(analysis);

    this.env = analysis.getTypeFactory().getProcessingEnv();
    this.aTypeFactory = (NonEmptyAnnotatedTypeFactory) analysis.getTypeFactory();

    this.collectionSize = TreeUtils.getMethod("java.util.Collection", "size", 0, this.env);
    this.mapSize = TreeUtils.getMethod("java.util.Map", "size", 0, this.env);
    this.listIndexOf = TreeUtils.getMethod("java.util.List", "indexOf", 1, this.env);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitEqualTo(
      EqualToNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitEqualTo(n, in);

    // The equality holds.
    strengthenAnnotationSizeEquals(
        in, n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    refineGTE(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    refineGTE(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());

    // The equality does not hold.
    refineNotEqual(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());
    refineNotEqual(n.getRightOperand(), n.getLeftOperand(), result.getElseStore());

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitNotEqual(
      NotEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitNotEqual(n, in);

    refineNotEqual(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    refineNotEqual(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());

    strengthenAnnotationSizeEquals(
        in, n.getLeftOperand(), n.getRightOperand(), result.getElseStore());

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThan(n, in);

    // A < B is equivalent to B > A.
    refineGT(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());

    // This handles the case where n < container.size().
    refineGTE(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
      LessThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(n, in);

    // A <= B is equivalent to B >= A.
    // This handles the case where n <= container.size()
    refineGTE(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());

    refineGT(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());

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

  @Override
  public TransferResult<CFValue, CFStore> visitCase(
      CaseNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitCase(n, in);
    List<Node> caseOperands = n.getCaseOperands();
    Node switchOpNode = n.getSwitchOperand().getExpression();

    refineSwitchStatement(switchOpNode, caseOperands, result.getThenStore(), result.getElseStore());

    return result;
  }

  /**
   * Refine the transfer result's store, given the left- and right-hand side of an equality check
   * comparing container sizes.
   *
   * @param in transfer input used to get the types of subnodes of {@code lhs} and {@code rhs}.
   * @param lhs a node that may be a method invocation of {@link java.util.Collection size()} or
   *     {@link java.util.Map size()}
   * @param rhs a node that may be a method invocation of {@link java.util.Collection size()} or
   *     {@link java.util.Map size()}
   * @param store the "then" store of the comparison operation
   */
  private void strengthenAnnotationSizeEquals(
      TransferInput<CFValue, CFStore> in, Node lhs, Node rhs, CFStore store) {
    if (!isSizeAccess(lhs) || !isSizeAccess(rhs)) {
      return;
    }

    if (isAccessOfNonEmptyCollection(in, (MethodInvocationNode) lhs)) {
      store.insertValue(getReceiverJE(rhs), aTypeFactory.NON_EMPTY);
    } else if (isAccessOfNonEmptyCollection(in, (MethodInvocationNode) rhs)) {
      store.insertValue(getReceiverJE(lhs), aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Returns true if the receiver of {@code methodAccessNode} is non-empty according to {@code in}.
   *
   * @param in used to get the type of {@code methodAccessNode}.
   * @param methodAccessNode method access
   * @return true if the receiver of {@code methodAccessNode} is non-empty according to {@code in}
   */
  private boolean isAccessOfNonEmptyCollection(
      TransferInput<CFValue, CFStore> in, MethodInvocationNode methodAccessNode) {
    Node receiver = methodAccessNode.getTarget().getReceiver();

    return aTypeFactory.containsSameByClass(
        in.getValueOfSubNode(receiver).getAnnotations(), NonEmpty.class);
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() != n}, {@code n != container.size()}, or {@code
   * container.indexOf(Object) != n}.
   *
   * <p>This method is always called twice, with the arguments reversed. So, it can do non-symmetric
   * checks.
   *
   * <p>For example, the type of {@code container} in the "then" branch of a conditional statement
   * with the test {@code container.size() != n} where {@code n} is 0 should refine to
   * {@code @NonEmpty}.
   *
   * <p>This method is also used to refine the "else" store of an equality comparison where {@code
   * container.size()} is compared against 0.
   *
   * @param left the left operand of a binary operation
   * @param right the right operand of a binary operation
   * @param store the abstract store to update
   */
  private void refineNotEqual(Node left, Node right, CFStore store) {
    if (!(right instanceof IntegerLiteralNode)) {
      return;
    }
    Integer emptyValue = emptyValue(left);
    if (emptyValue == null) {
      return;
    }
    // In case of a size() comparison, refine the store if the value is 0
    // In case of a indexOf(Object) check, refine the store if the value is -1
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) right;
    if (integerLiteralNode.getValue() == (int) emptyValue) {
      store.insertValue(getReceiverJE(left), aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() > n} or {@code container.indexOf(Object) > n}.
   *
   * <p>When this method is called, {@link refineGTE} is also called, with the arguments reversed.
   * So, this method can do non-symmetric checks.
   *
   * <p>For example, the type of {@code container} in the "then" branch of a conditional statement
   * with the test {@code container.size() > n} where {@code n >= 0} should be refined to
   * {@code @NonEmpty}.
   *
   * @param left the left operand of a binary operation
   * @param right the right operand of a binary operation
   * @param store the abstract store to update
   */
  private void refineGT(Node left, Node right, CFStore store) {
    if (!(right instanceof IntegerLiteralNode)) {
      return;
    }
    Integer emptyValue = emptyValue(left);
    if (emptyValue == null) {
      return;
    }
    // In case of a size() comparison, refine the store if the value is 0
    // In case of a indexOf(Object) check, refine the store if the value is -1
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) right;
    if (integerLiteralNode.getValue() >= (int) emptyValue) {
      store.insertValue(getReceiverJE(left), aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() >= n} or {@code container.indexOf(Object) >=
   * n}.
   *
   * <p>When this method is called, {@link refineGTE} is also called, with the arguments reversed.
   * So, this method can do non-symmetric checks.
   *
   * <p>For example, the type of {@code container} in the "then" branch of a conditional statement
   * with the test {@code container.size() >= n} where {@code n > 0} should be refined to
   * {@code @NonEmpty}.
   *
   * <p>This method is also used to refine the "then" branch of an equality comparison where {@code
   * container.size()} is compared against a non-zero value.
   *
   * @param left the left operand of a binary operation
   * @param right the right operand of a binary operation
   * @param store the abstract store to update
   */
  private void refineGTE(Node left, Node right, CFStore store) {
    if (!(right instanceof IntegerLiteralNode)) {
      return;
    }
    Integer emptyValue = emptyValue(left);
    if (emptyValue == null) {
      return;
    }
    // In case of a size() comparison, refine the store if the value is 0
    // In case of a indexOf(Object) check, refine the store if the value is -1
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) right;
    if (integerLiteralNode.getValue() > (int) emptyValue) {
      store.insertValue(getReceiverJE(left), aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for switch
   * statements, where the test expression is of the form {@code container.size()} or {@code
   * container.indexOf(Object)}.
   *
   * <p>For example, the "then" store of any case node with an integer value greater than 0 (or -1,
   * in the case of the test expression being a call to {@code container.indexOf(Object)}) should
   * refine the type of {@code container} to {@code @NonEmpty}.
   *
   * @param testNode a node that is the test expression for a {@code switch} statement
   * @param caseOperands the operands within each case label
   * @param thenStore the "then" store
   * @param elseStore the "else" store, corresponding to the "default" case label
   */
  private void refineSwitchStatement(
      Node testNode, List<Node> caseOperands, CFStore thenStore, CFStore elseStore) {
    Integer emptyValue = emptyValue(testNode);
    if (emptyValue == null) {
      return;
    }
    for (Node caseOperand : caseOperands) {
      if (!(caseOperand instanceof IntegerLiteralNode)) {
        continue;
      }
      IntegerLiteralNode caseIntegerLiteral = (IntegerLiteralNode) caseOperand;
      JavaExpression receiver = getReceiverJE(testNode);
      CFStore storeToUpdate =
          caseIntegerLiteral.getValue() > (int) emptyValue ? thenStore : elseStore;
      storeToUpdate.insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Return true if the given node is an invocation of {@link java.util.Collection#size()} or {@link
   * java.util.Map#size()}.
   *
   * @param possibleSizeAccess a node that may be a method call to the {@code size()} method in the
   *     {@link java.util.Collection} or {@link java.util.Map} types
   * @return true if the node is a method call to size()
   */
  private boolean isSizeAccess(Node possibleSizeAccess) {
    return NodeUtils.isMethodInvocation(possibleSizeAccess, collectionSize, env)
        || NodeUtils.isMethodInvocation(possibleSizeAccess, mapSize, env);
  }

  /**
   * Return the receiver as a {@link JavaExpression} given a method invocation node.
   *
   * @param node a method invocation
   * @return the receiver as a {@link JavaExpression}
   */
  private JavaExpression getReceiverJE(Node node) {
    MethodAccessNode methodAccessNode = ((MethodInvocationNode) node).getTarget();
    return JavaExpression.fromNode(methodAccessNode.getReceiver());
  }

  /**
   * If this is an invocation of a size-dependent method, return the value that the method returns
   * for an empty container.
   *
   * @param n a node that might be an invocation of a size-dependent method
   * @return the value that the method returns ffor an empty container, or null
   */
  private Integer emptyValue(Node n) {
    if (isSizeAccess(n)) {
      return 0;
    } else if (NodeUtils.isMethodInvocation(n, listIndexOf, env)) {
      return -1;
    } else {
      return null;
    }
  }
}
