package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for string concatenation:
 *
 * <pre>
 *   <em>expression</em> + <em>expression</em>
 * </pre>
 */
public class StringConcatenateNode extends BinaryOperationNode {

  /**
   * Constructs a {@link StringConcatenateNode}.
   *
   * @param tree the binary tree
   * @param left the left operand
   * @param right the right operand
   */
  public StringConcatenateNode(BinaryTree tree, Node left, Node right) {
    super(tree, left, right);
    assert tree.getKind() == Tree.Kind.PLUS;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitStringConcatenate(this, p);
  }

  @Override
  public String toString() {
    return "(" + getLeftOperand() + " + " + getRightOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof StringConcatenateNode)) {
      return false;
    }
    StringConcatenateNode other = (StringConcatenateNode) obj;
    return getLeftOperand().equals(other.getLeftOperand())
        && getRightOperand().equals(other.getRightOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLeftOperand(), getRightOperand());
  }
}
