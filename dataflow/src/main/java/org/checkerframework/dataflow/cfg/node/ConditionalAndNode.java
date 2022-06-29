package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for a conditional and expression:
 *
 * <pre>
 *   <em>expression</em> &amp;&amp; <em>expression</em>
 * </pre>
 */
public class ConditionalAndNode extends BinaryOperationNode {

  /**
   * Create a new ConditionalAndNode.
   *
   * @param tree the conditional-and tree for this node
   * @param left the first argument
   * @param right the second argument
   */
  public ConditionalAndNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.CONDITIONAL_AND;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitConditionalAnd(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " && " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ConditionalAndNode)) {
      return false;
    }
    ConditionalAndNode other = (ConditionalAndNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
