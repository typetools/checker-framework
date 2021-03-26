package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the null literal.
 *
 * <pre>
 *   <em>null</em>
 * </pre>
 */
public class NullLiteralNode extends ValueLiteralNode {

  /**
   * Create a new NullLiteralNode.
   *
   * @param t the tree for the literal value
   */
  public NullLiteralNode(LiteralTree t) {
    super(t);
    assert t.getKind() == Tree.Kind.NULL_LITERAL;
  }

  @Override
  public Void getValue() {
    return (Void) tree.getValue();
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitNullLiteral(this, p);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof NullLiteralNode)) {
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
