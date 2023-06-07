package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

/** A CatchMarkerNode is a marker node for the beginning or end of a catch block. */
public class CatchMarkerNode extends MarkerNode {

  /** The type of the exception parameter. */
  private final TypeMirror catchType;

  /** The type utilities. */
  private final Types types;

  /**
   * Creates a new CatchMarkerNode.
   *
   * @param tree the tree
   * @param startOrEnd {@code "start"} or {@code "end"}
   * @param catchType the type of the exception parameter
   * @param types the type utilities
   */
  public CatchMarkerNode(
      @Nullable Tree tree, String startOrEnd, TypeMirror catchType, Types types) {
    super(
        tree,
        startOrEnd + " of catch block for " + catchType + " #" + TreeUtils.treeUids.get(tree),
        types);
    this.catchType = catchType;
    this.types = types;
  }

  /**
   * Returns the type of the exception parameter.
   *
   * @return the type of the exception parameter
   */
  public TypeMirror getCatchType() {
    return catchType;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof CatchMarkerNode)) {
      return false;
    }
    CatchMarkerNode other = (CatchMarkerNode) obj;
    return types.isSameType(getCatchType(), other.getCatchType()) && super.equals(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tree, getMessage(), catchType);
  }
}
