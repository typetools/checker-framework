package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for a short literal. For example:
 *
 * <pre>
 *   <em>5</em>
 *   <em>0x8fff</em>
 * </pre>
 *
 * Java source and the AST representation do not have "short" literals. They have integer literals
 * that may be narrowed to shorts depending on context.
 */
// TODO: If we use explicit NarrowingConversionNodes, do we need ShortLiteralNodes too?
public class ShortLiteralNode extends ValueLiteralNode {

  /**
   * Create a new ShortLiteralNode.
   *
   * @param t the tree for the literal value
   */
  public ShortLiteralNode(LiteralTree t) {
    super(t);
    assert t.getKind() == Tree.Kind.INT_LITERAL;
  }

  @Override
  public Short getValue() {
    return (Short) tree.getValue();
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitShortLiteral(this, p);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    // test that obj is a ShortLiteralNode
    if (!(obj instanceof ShortLiteralNode)) {
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
