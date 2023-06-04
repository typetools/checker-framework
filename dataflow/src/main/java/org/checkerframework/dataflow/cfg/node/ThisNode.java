package org.checkerframework.dataflow.cfg.node;

import java.util.Collection;
import java.util.Collections;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * A node for a reference to 'this', either implicit or explicit.
 *
 * <pre>
 *   <em>this</em>
 * </pre>
 */
public abstract class ThisNode extends Node {

  protected ThisNode(TypeMirror type) {
    super(type);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof ThisNode;
  }

  @Override
  public int hashCode() {
    return 3559101; // Objects.hash("this");
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.emptyList();
  }
}
