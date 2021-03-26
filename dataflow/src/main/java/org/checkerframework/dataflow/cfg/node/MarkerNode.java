package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MarkerNodes are no-op Nodes used for debugging information. They can hold a Tree and a message,
 * which will be part of the String representation of the MarkerNode.
 *
 * <p>An example use case for MarkerNodes is representing switch statements.
 */
public class MarkerNode extends Node {

  protected final @Nullable Tree tree;
  protected final String message;

  public MarkerNode(@Nullable Tree tree, String message, Types types) {
    super(types.getNoType(TypeKind.NONE));
    this.tree = tree;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public @Nullable Tree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitMarker(this, p);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("marker (");
    sb.append(message);
    sb.append(")");
    return sb.toString();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof MarkerNode)) {
      return false;
    }
    MarkerNode other = (MarkerNode) obj;
    return Objects.equals(getTree(), other.getTree()) && getMessage().equals(other.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(tree, getMessage());
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.emptyList();
  }
}
