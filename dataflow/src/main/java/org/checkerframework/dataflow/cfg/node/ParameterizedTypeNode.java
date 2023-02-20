package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ParameterizedTypeTree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a parameterized type occurring in an expression:
 *
 * <pre>
 *   <em>type&lt;arg1, arg2&gt;</em>
 * </pre>
 *
 * Parameterized types don't represent any computation to be done at runtime, so we might choose to
 * represent them differently by modifying the {@link Node}s in which parameterized types can occur,
 * such as {@link ObjectCreationNode}s.
 */
public class ParameterizedTypeNode extends Node {

  protected final ParameterizedTypeTree tree;

  public ParameterizedTypeNode(ParameterizedTypeTree t) {
    super(TreeUtils.typeOf(t));
    tree = t;
  }

  @Override
  public ParameterizedTypeTree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitParameterizedType(this, p);
  }

  @Override
  public String toString() {
    return getTree().toString();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ParameterizedTypeNode)) {
      return false;
    }
    ParameterizedTypeNode other = (ParameterizedTypeNode) obj;
    return getTree().equals(other.getTree());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTree());
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.emptyList();
  }
}
