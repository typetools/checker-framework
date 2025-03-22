package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnyPatternNode extends Node {

  private final Tree anyPatternTree;

  public AnyPatternNode(TypeMirror type, Tree anyPatternTree) {
    super(type);
    this.anyPatternTree = anyPatternTree;
  }

  @Override
  public @Nullable Tree getTree() {
    return anyPatternTree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitAnyPattern(this, p);
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.emptySet();
  }
}
