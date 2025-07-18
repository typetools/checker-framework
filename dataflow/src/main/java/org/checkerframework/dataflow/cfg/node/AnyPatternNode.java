package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

/**
 * A node for an any pattern, which is "{@code _}" in "{@code case _ -> ...code that ignores the
 * switched value...}" or "{@code case MyType(_) -> ...}".
 */
public class AnyPatternNode extends Node {

  /**
   * The {@code AnyPatternTree}, declared as {@link Tree} to permit this file to compile under JDK
   * 21 and earlier.
   */
  private final Tree anyPatternTree;

  /**
   * Creates a {@code AnyPatternNode}.
   *
   * @param type the type of the node
   * @param anyPatternTree the {@code AnyPatternTree}
   */
  public AnyPatternNode(TypeMirror type, Tree anyPatternTree) {
    super(type);
    this.anyPatternTree = anyPatternTree;
  }

  @Override
  @Pure
  public @Nullable Tree getTree() {
    return anyPatternTree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitAnyPattern(this, p);
  }

  @Override
  @Pure
  public Collection<Node> getOperands() {
    return Collections.emptySet();
  }

  @Override
  public String toString() {
    return "_";
  }
}
