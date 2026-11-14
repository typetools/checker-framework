package org.checkerframework.framework.testchecker.promotion;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.promotion.qual.Promoted;
import org.checkerframework.framework.type.AnnotatedTypeFactory.BinaryArgTypes;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * The visitor for the Promotion Checker. It reports {@code promoted.left} or {@code promoted.right}
 * for each operand of a binary operation or compound assignment that {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#binaryTreeArgTypes} unboxes or widens.
 */
public class PromotionVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /**
   * Creates a PromotionVisitor.
   *
   * @param checker a Promotion Checker
   */
  public PromotionVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitBinary(BinaryTree tree, Void p) {
    reportPromotedOperands(
        tree,
        atypeFactory.binaryTreeArgTypes(tree),
        atypeFactory.getAnnotatedType(tree.getLeftOperand()),
        atypeFactory.getAnnotatedType(tree.getRightOperand()));
    return super.visitBinary(tree, p);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    reportPromotedOperands(
        tree,
        atypeFactory.compoundAssignmentTreeArgTypes(tree),
        atypeFactory.getAnnotatedType(tree.getVariable()),
        atypeFactory.getAnnotatedType(tree.getExpression()));
    return super.visitCompoundAssignment(tree, p);
  }

  /**
   * Reports an error for each operand of the given operation that promotion converts. These errors
   * have no corresponding text in a messages.properties file; this checker is just for testing.
   *
   * @param tree a binary operation or compound assignment
   * @param argTypes the types of the operands of {@code tree}, after promotion
   * @param left the type of the left operand of {@code tree}, before promotion
   * @param right the type of the right operand of {@code tree}, before promotion
   */
  private void reportPromotedOperands(
      Tree tree, BinaryArgTypes argTypes, AnnotatedTypeMirror left, AnnotatedTypeMirror right) {
    if (isPromoted(argTypes.left(), left)) {
      checker.reportError(tree, "promoted.left");
    }
    if (isPromoted(argTypes.right(), right)) {
      checker.reportError(tree, "promoted.right");
    }
  }

  /**
   * Returns true if promotion converted the given operand. An operand that is already
   * {@code @Promoted} before promotion, such as the result of a nested operation whose operands
   * were promoted, is not reported unless promotion changes its Java type.
   *
   * @param converted the type of an operand, after promotion
   * @param original the type of the same operand, before promotion
   * @return true if promotion converted the operand
   */
  private boolean isPromoted(AnnotatedTypeMirror converted, AnnotatedTypeMirror original) {
    return converted.hasPrimaryAnnotation(Promoted.class)
        && !types.isSameType(converted.getUnderlyingType(), original.getUnderlyingType());
  }
}
