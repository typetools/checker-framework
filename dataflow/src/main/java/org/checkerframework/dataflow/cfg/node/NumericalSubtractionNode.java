package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the numerical subtraction:
 *
 * <pre>
 *   <em>expression</em> - <em>expression</em>
 * </pre>
 */
public class NumericalSubtractionNode extends BinaryOperationNode {

  /**
   * Constructs a {@link NumericalSubtractionNode}
   *
   * @param tree The binary tree
   * @param left The left-hand side
   * @param right The right-hand side
   */
  public NumericalSubtractionNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.MINUS;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNumericalSubtraction(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " - " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof NumericalSubtractionNode)) {
      return false;
    }
    NumericalSubtractionNode other = (NumericalSubtractionNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
