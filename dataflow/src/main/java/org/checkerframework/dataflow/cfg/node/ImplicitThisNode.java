package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A node to model the implicit {@code this}, e.g., in a field access. */
public class ImplicitThisNode extends ThisNode {

  public ImplicitThisNode(TypeMirror type) {
    super(type);
  }

  @Override
  public @Nullable Tree getTree() {
    return null;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitImplicitThis(this, p);
  }

  // In an inner class context, an implicit this may need to be represented as "Outer.this" rather
  // than just as "this".  This is context-dependent, and toString doesn't know if it is being
  // used in an inner class context.
  @Override
  public String toString() {
    if (Node.disambiguateOwner) {
      return "(this{owner=" + type + "})";
    } else {
      return "(this)";
    }
  }
}
