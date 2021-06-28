package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the integer division:
 *
 * <pre>
 *   <em>expression</em> / <em>expression</em>
 * </pre>
 */
public class IntegerDivisionNode extends BinaryOperationNode {

  public IntegerDivisionNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.DIVIDE;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitIntegerDivision(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " / " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof IntegerDivisionNode)) {
      return false;
    }
    IntegerDivisionNode other = (IntegerDivisionNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
