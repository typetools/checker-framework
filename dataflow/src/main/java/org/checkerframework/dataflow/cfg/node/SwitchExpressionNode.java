package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SwitchExpressionNode extends Node {

  /**
   * Creates a new Node.
   *
   * @param type the type of the node
   */
  protected SwitchExpressionNode(TypeMirror type) {
    super(type);
  }

  @Override
  public @Nullable Tree getTree() {
    return null;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return null;
  }

  @Override
  public Collection<Node> getOperands() {
    return null;
  }
}
