package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the greater than comparison:
 *
 * <pre>
 *   <em>expression</em> &gt; <em>expression</em>
 * </pre>
 */
public class GreaterThanNode extends BinaryOperationNode {

  /**
   * Constructs a {@link GreaterThanNode}
   *
   * @param tree the binary tree
   * @param left the left operand
   * @param right the right operand
   */
  public GreaterThanNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.GREATER_THAN;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitGreaterThan(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " > " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof GreaterThanNode)) {
      return false;
    }
    GreaterThanNode other = (GreaterThanNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
