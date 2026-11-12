package org.checkerframework.framework.testchecker.promotion;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.promotion.qual.Promoted;
import org.checkerframework.framework.type.AnnotatedTypeFactory.BinaryArgTypes;

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
    reportPromotedOperands(tree, atypeFactory.binaryTreeArgTypes(tree));
    return super.visitBinary(tree, p);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    reportPromotedOperands(tree, atypeFactory.compoundAssignmentTreeArgTypes(tree));
    return super.visitCompoundAssignment(tree, p);
  }

  /**
   * Reports an error for each promoted operand of the given operation. These errors have no
   * corresponding text in a messages.properties file; this checker is just for testing.
   *
   * @param tree a binary operation or compound assignment
   * @param argTypes the types of the operands of {@code tree}, after promotion
   */
  private void reportPromotedOperands(Tree tree, BinaryArgTypes argTypes) {
    if (argTypes.left().hasPrimaryAnnotation(Promoted.class)) {
      checker.reportError(tree, "promoted.left");
    }
    if (argTypes.right().hasPrimaryAnnotation(Promoted.class)) {
      checker.reportError(tree, "promoted.right");
    }
  }
}
