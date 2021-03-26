package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for an integer literal. For example:
 *
 * <pre>
 *   <em>42</em>
 * </pre>
 */
public class IntegerLiteralNode extends ValueLiteralNode {

  /**
   * Create a new IntegerLiteralNode.
   *
   * @param t the tree for the literal value
   */
  public IntegerLiteralNode(LiteralTree t) {
    super(t);
    assert t.getKind() == Tree.Kind.INT_LITERAL;
  }

  @Override
  public Integer getValue() {
    return (Integer) tree.getValue();
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitIntegerLiteral(this, p);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    // test that obj is a IntegerLiteralNode
    if (!(obj instanceof IntegerLiteralNode)) {
      return false;
    }
    // super method compares values
    return super.equals(obj);
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.emptyList();
  }
}
