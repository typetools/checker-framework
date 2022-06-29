package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for a conditional not expression:
 *
 * <pre>
 *   ! <em>expression</em>
 * </pre>
 */
public class ConditionalNotNode extends UnaryOperationNode {

  /**
   * Create a new ConditionalNotNode.
   *
   * @param tree the logical-complement tree for this node
   * @param operand the boolean expression being negated
   */
  public ConditionalNotNode(UnaryTree tree, Node operand) {
    super(tree, operand);
    assert tree.getKind() == Tree.Kind.LOGICAL_COMPLEMENT;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitConditionalNot(this, p);
  }

  @Override
  public String toString() {
    return "(!" + getOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ConditionalNotNode)) {
      return false;
    }
    ConditionalNotNode other = (ConditionalNotNode) obj;
    return getOperand().equals(other.getOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(ConditionalNotNode.class, getOperand());
  }
}
