package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the bitwise or logical (single bit) or operation:
 *
 * <pre>
 *   <em>expression</em> | <em>expression</em>
 * </pre>
 */
public class BitwiseOrNode extends BinaryOperationNode {

  /**
   * Constructs a {@link BitwiseOrNode}.
   *
   * @param tree the binary tree
   * @param left the left operand
   * @param right the right operand
   */
  public BitwiseOrNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.OR;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitBitwiseOr(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " | " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof BitwiseOrNode)) {
      return false;
    }
    BitwiseOrNode other = (BitwiseOrNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
