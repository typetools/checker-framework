package org.checkerframework.checker.nonempty;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nonempty.qual.Delegate;
import org.checkerframework.checker.nonempty.qual.EnsuresNonEmpty;
import org.checkerframework.checker.nonempty.qual.EnsuresNonEmptyIf;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreePathUtil;
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

  /** The {@code size()} method of the {@link java.util.Map} class. */
  private final ExecutableElement mapSize;

  /** The {@code indexOf(Object)} method of the {@link java.util.List} class. */
  private final ExecutableElement indexOf;

  /** A {@link NonEmptyAnnotatedTypeFactory} instance. */
  private final NonEmptyAnnotatedTypeFactory aTypeFactory;

  public NonEmptyTransfer(CFAnalysis analysis) {
    super(analysis);

    this.env = analysis.getTypeFactory().getProcessingEnv();
    this.collectionSize = TreeUtils.getMethod("java.util.Collection", "size", 0, this.env);
    this.mapSize = TreeUtils.getMethod("java.util.Map", "size", 0, this.env);
    this.indexOf = TreeUtils.getMethod("java.util.List", "indexOf", 1, this.env);
    this.aTypeFactory = (NonEmptyAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
    MethodTree enclosingMethodTree = TreePathUtil.enclosingMethod(n.getTreePath());
    if (enclosingMethodTree == null || TreeUtils.isConstructor(enclosingMethodTree)) {
      return result;
    }
    Tree receiverTree = n.getTarget().getReceiver().getTree();
    if (receiverTree == null) {
      return result;
    }
    Element receiver = TreeUtils.elementFromTree(receiverTree);
    if (!shouldRefineStoreForDelegationInvocation(receiver, enclosingMethodTree)) {
      return result;
    }
    JavaExpression thisExpr = JavaExpression.getImplicitReceiver(receiver);
    refineStoreForDelegationInvocation(
        thisExpr, JavaExpression.fromNode(n.getTarget().getReceiver()), result);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitEqualTo(
      EqualToNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitEqualTo(n, in);
    // Account for the case where the sizes of two containers are compared
    strengthenAnnotationSizeEquals(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    // Account for the case where size is checked against a non-zero integer
    refineGTE(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    refineGTE(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());
    // A == 0 is the inversion of A != 0
    refineNotEqual(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());
    refineNotEqual(n.getRightOperand(), n.getLeftOperand(), result.getElseStore());
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitNotEqual(
      NotEqualNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitNotEqual(n, in);
    strengthenAnnotationSizeEquals(n.getLeftOperand(), n.getRightOperand(), result.getElseStore());
    refineNotEqual(n.getLeftOperand(), n.getRightOperand(), result.getThenStore());
    refineNotEqual(n.getRightOperand(), n.getLeftOperand(), result.getThenStore());
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

  @Override
  public TransferResult<CFValue, CFStore> visitCase(
      CaseNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitCase(n, in);
    List<Node> caseOperands = n.getCaseOperands();
    AssignmentNode assign = n.getSwitchOperand();
    Node switchNode = assign.getExpression();
    refineSwitchStatement(switchNode, caseOperands, result.getThenStore(), result.getElseStore());
    return result;
  }

  /**
   * Return true if the transfer store for "this" should be updated, depending on whether a delegate
   * method invocation is found within a method body.
   *
   * <p>Note: the Non-Empty Checker trusts the {@link Delegate} annotations it finds. The {@link
   * DelegationChecker} verifies correct use of the delegation pattern. Since it is run alongside
   * the Non-Empty Checker, the annotations it finds should be correct.
   *
   * @param receiver the receiver of a candidate delegate method call found in a method body
   * @param enclosingMethodTree the method enclosing the candidate delegate call
   * @return true if the receiver is annotated with {@link Delegate} and the method is annotated
   *     with a postcondition annotation from the Non-Empty type system.
   */
  private boolean shouldRefineStoreForDelegationInvocation(
      Element receiver, MethodTree enclosingMethodTree) {
    Element enclosingMethod = TreeUtils.elementFromDeclaration(enclosingMethodTree);
    AnnotationMirror delegateAnno = aTypeFactory.getDeclAnnotation(receiver, Delegate.class);
    AnnotationMirror postConditionAnno =
        aTypeFactory.getDeclAnnotation(enclosingMethod, EnsuresNonEmpty.class);
    AnnotationMirror conditionalPostconditionAnno =
        aTypeFactory.getDeclAnnotation(enclosingMethod, EnsuresNonEmptyIf.class);
    return delegateAnno != null
        && (postConditionAnno != null || conditionalPostconditionAnno != null);
  }

  /**
   * Updates the value in the store for the target expression when a delegate call is detected.
   *
   * <p>For example, if a field {@code map} is marked with {@link Delegate}, and the enclosing class
   * delegates a call to it (e.g., a call to {@code containsValue(Object)}), then an instance of the
   * enclosing class should have the same postconditions that hold for {@code map}.
   *
   * @param targetExpr the value for which the store should be updated
   * @param delegate the delegate field
   * @param result the transfer result
   */
  private void refineStoreForDelegationInvocation(
      JavaExpression targetExpr, JavaExpression delegate, TransferResult<CFValue, CFStore> result) {
    if (result.containsTwoStores()) {
      // Update the "then" store
      CFStore thenStore = result.getThenStore();
      CFValue delegateThenStoreValue = thenStore.getValue(delegate);
      thenStore.replaceValue(targetExpr, delegateThenStoreValue);

      // Update the "else" store
      CFStore elseStore = result.getElseStore();
      CFValue delegateElseStoreValue = elseStore.getValue(delegate);
      elseStore.replaceValue(targetExpr, delegateElseStoreValue);
    } else {
      CFStore store = result.getRegularStore();
      CFValue delegateStoreValue = store.getValue(delegate);
      store.replaceValue(targetExpr, delegateStoreValue);
    }
  }

  /**
   * Refine the transfer result's store, given the left- and right-hand side of an equality check
   * comparing container sizes.
   *
   * @param lhs a node that may be a method invocation for {@link java.util.Collection size()} or
   *     {@link java.util.Map size()}
   * @param rhs a node that may be a method invocation for {@link java.util.Collection size()} or
   *     {@link java.util.Map size()}
   * @param store the "then" store of the comparison operation
   */
  private void strengthenAnnotationSizeEquals(Node lhs, Node rhs, CFStore store) {
    if (!isSizeAccess(lhs) || !isSizeAccess(rhs)) {
      return;
    }
    AnnotationMirror lhsNonEmptyAnno =
        aTypeFactory.getAnnotationFromJavaExpression(
            getReceiver(lhs), lhs.getTree(), NonEmpty.class);
    AnnotationMirror rhsNonEmptyAnno =
        aTypeFactory.getAnnotationFromJavaExpression(
            getReceiver(rhs), rhs.getTree(), NonEmpty.class);
    // TODO: use aTypeFactory.getQualifierHierarchy().greatestLowerBoundQualifiersOnly() ?
    if (lhsNonEmptyAnno != null) {
      store.insertValue(getReceiver(rhs), aTypeFactory.NON_EMPTY);
    } else if (rhsNonEmptyAnno != null) {
      store.insertValue(getReceiver(lhs), aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() != n}, {@code n != container.size()}, or {@code
   * container.indexOf(Object) != n}.
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
    boolean isSizeComparison = isSizeComparison(left, right);
    boolean isIndexOfComparison = isIndexOfComparison(left, right);
    if (!isSizeComparison && !isIndexOfComparison) {
      return;
    }
    // In case of a size() comparison, refine the store if the value is 0
    // In case of a indexOf(Object) check, refine the store if the value is -1
    int threshold = isSizeComparison ? 0 : -1;
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) right;
    if (integerLiteralNode.getValue() == threshold) {
      JavaExpression receiver = getReceiver(left);
      store.insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() > n} or {@code container.indexOf(Object) > n}.
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
    boolean isSizeComparison = isSizeComparison(left, right);
    boolean isIndexOfComparison = isIndexOfComparison(left, right);
    if (!isSizeComparison && !isIndexOfComparison) {
      return;
    }
    // In case of a size() comparison, refine the store if the value is 0
    // In case of a indexOf(Object) check, refine the store if the value is -1
    int threshold = isSizeComparison ? 0 : -1;
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) right;
    if (integerLiteralNode.getValue() >= threshold) {
      JavaExpression receiver = getReceiver(left);
      store.insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Updates the transfer result's store with information from the Non-Empty type system for
   * expressions of the form {@code container.size() >= n} or {@code container.indexOf(Object) >=
   * n}.
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
    boolean isSizeComparison = isSizeComparison(left, right);
    boolean isIndexOfComparison = isIndexOfComparison(left, right);
    if (!isSizeComparison && !isIndexOfComparison) {
      return;
    }
    IntegerLiteralNode integerLiteralNode = (IntegerLiteralNode) right;
    JavaExpression receiver = getReceiver(left);
    // In an indexOf(Object) comparison, if the index is GTE 0, then the object is within the
    // container
    if (isIndexOfComparison && integerLiteralNode.getValue() >= 0) {
      store.insertValue(receiver, aTypeFactory.NON_EMPTY);
      return;
    }
    if (integerLiteralNode.getValue() > 0) {
      store.insertValue(receiver, aTypeFactory.NON_EMPTY);
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
    boolean isIndexOfAccess = NodeUtils.isMethodInvocation(testNode, indexOf, env);
    if (!isSizeAccess(testNode) && !isIndexOfAccess) {
      return;
    }
    for (Node caseOperand : caseOperands) {
      if (!(caseOperand instanceof IntegerLiteralNode)) {
        continue;
      }
      IntegerLiteralNode caseIntegerLiteral = (IntegerLiteralNode) caseOperand;
      JavaExpression receiver = getReceiver(testNode);
      CFStore storeToUpdate;
      if (isIndexOfAccess) {
        storeToUpdate = caseIntegerLiteral.getValue() >= 0 ? thenStore : elseStore;
      } else {
        storeToUpdate = caseIntegerLiteral.getValue() > 0 ? thenStore : elseStore;
      }
      storeToUpdate.insertValue(receiver, aTypeFactory.NON_EMPTY);
    }
  }

  /**
   * Check whether a given binary operation corresponds to a {@link java.util.List size()} or {@link
   * java.util.Map size()}comparison.
   *
   * @param left the left operand of a binary operation
   * @param right the right operand of a binary operation
   * @return true if the operands correspond to a {@link java.util.List size()} or {@link
   *     java.util.Map size()} comparison
   */
  private boolean isSizeComparison(Node left, Node right) {
    // Use `List.of()` in Java 9+
    return isSizeAccess(left) && right instanceof IntegerLiteralNode;
  }

  /**
   * Return true if the given node is an instance of a method invocation node for {@link
   * java.util.Collection size()} or {@link java.util.Map size()}.
   *
   * @param possibleSizeAccess a node that may be a method call to the {@code size()} method in the
   *     {@link java.util.Collection} or {@link java.util.Map} types
   * @return true if the node is a method call to size()
   */
  private boolean isSizeAccess(Node possibleSizeAccess) {
    // In Java 9+, use `List.of()`
    List<ExecutableElement> sizeAccessMethods = Arrays.asList(collectionSize, mapSize);
    return sizeAccessMethods.stream()
        .anyMatch(
            sizeAccessMethod ->
                NodeUtils.isMethodInvocation(possibleSizeAccess, sizeAccessMethod, env));
  }

  /**
   * Check whether a given binary operation corresponds to a {@link java.util.List indexOf(Object)}
   * comparison.
   *
   * @param left the left operand of a binary operation
   * @param right the right operand of a binary operation
   * @return true if the operands correspond to a {@link java.util.List indexOf(Object)} comparison.
   */
  private boolean isIndexOfComparison(Node left, Node right) {
    return NodeUtils.isMethodInvocation(left, indexOf, env) && right instanceof IntegerLiteralNode;
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
