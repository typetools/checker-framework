package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for bitwise left shift operations:
 *
 * <pre>
 *   <em>expression</em> &lt;&lt; <em>expression</em>
 * </pre>
 */
public class LeftShiftNode extends BinaryOperationNode {

  public LeftShiftNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Kind.LEFT_SHIFT;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitLeftShift(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " << " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof LeftShiftNode)) {
      return false;
    }
    LeftShiftNode other = (LeftShiftNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
