package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a reference to 'super'.
 *
 * <pre>
 *   <em>super</em>
 * </pre>
 *
 * Its {@link #type} field is the type of the class in which "super" appears, <em>not<em> the type
 * to which the "super" identifier resolves.
 */
public class SuperNode extends Node {

  protected final IdentifierTree tree;

  public SuperNode(IdentifierTree tree) {
    super(TreeUtils.typeOf(tree));
    assert tree.getName().contentEquals("super");
    this.tree = tree;
    System.out.printf(
        "Created SuperNode: tree=%s [%s], type=%s [%s]%n",
        tree, tree.getClass(), type, type.getClass());
  }

  @Override
  public IdentifierTree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitSuper(this, p);
  }

  @Override
  public String toString() {
    return "super";
  }

  @Override
  public String toStringDisambiguated() {
    return "super{owner=" + type + "}";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof SuperNode;
  }

  @Override
  public int hashCode() {
    return 109801370; // Objects.hash("super");
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.emptyList();
  }
}
