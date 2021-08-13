package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the not equal comparison:
 *
 * <pre>
 *   <em>expression</em> != <em>expression</em>
 * </pre>
 */
public class NotEqualNode extends BinaryOperationNode {

  /**
   * Constructs a {@link NotEqualNode}.
   *
   * @param tree the binary tree
   * @param left the left operand
   * @param right the right operand
   */
  public NotEqualNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.NOT_EQUAL_TO;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNotEqual(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " != " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof NotEqualNode)) {
      return false;
    }
    NotEqualNode other = (NotEqualNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
