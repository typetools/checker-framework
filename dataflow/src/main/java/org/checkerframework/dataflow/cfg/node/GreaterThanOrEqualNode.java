package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the greater than or equal comparison:
 *
 * <pre>
 *   <em>expression</em> &gt;= <em>expression</em>
 * </pre>
 */
public class GreaterThanOrEqualNode extends BinaryOperationNode {

  /**
   * Constructs a {@link GreaterThanOrEqualNode}
   *
   * @param tree the binary tree
   * @param left the left operand
   * @param right the right operand
   */
  public GreaterThanOrEqualNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.GREATER_THAN_EQUAL;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitGreaterThanOrEqual(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " >= " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof GreaterThanOrEqualNode)) {
      return false;
    }
    GreaterThanOrEqualNode other = (GreaterThanOrEqualNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
