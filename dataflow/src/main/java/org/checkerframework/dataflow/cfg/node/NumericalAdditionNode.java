package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the numerical addition:
 *
 * <pre>
 *   <em>expression</em> + <em>expression</em>
 * </pre>
 */
public class NumericalAdditionNode extends BinaryOperationNode {

  /**
   * Constructs a {@link NumericalAdditionNode}
   *
   * @param tree the binary tree
   * @param left the left operand
   * @param right the right operand
   */
  public NumericalAdditionNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.PLUS || tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNumericalAddition(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " + " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof NumericalAdditionNode)) {
      return false;
    }
    NumericalAdditionNode other = (NumericalAdditionNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
