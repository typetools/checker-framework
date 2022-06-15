package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for an equality check:
 *
 * <pre>
 *   <em>expression</em> == <em>expression</em>
 * </pre>
 */
public class EqualToNode extends BinaryOperationNode {

  /**
   * Create a new EqualToNode object.
   *
   * @param tree the tree for this node
   * @param left the first argument
   * @param right the second argument
   */
  public EqualToNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.EQUAL_TO;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitEqualTo(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " == " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof EqualToNode)) {
      return false;
    }
    EqualToNode other = (EqualToNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
