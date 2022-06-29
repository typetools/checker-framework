package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for a conditional or expression:
 *
 * <pre>
 *   <em>expression</em> || <em>expression</em>
 * </pre>
 */
public class ConditionalOrNode extends BinaryOperationNode {

  /**
   * Create a new ConditionalOrNode.
   *
   * @param tree the conditional-or tree for this node
   * @param left the first argument
   * @param right the second argument
   */
  public ConditionalOrNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.CONDITIONAL_OR;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitConditionalOr(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " || " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ConditionalOrNode)) {
      return false;
    }
    ConditionalOrNode other = (ConditionalOrNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
