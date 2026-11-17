package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Pure;

/** A regular basic block that contains a sequence of {@link Node}s. */
public interface RegularBlock extends SingleSuccessorBlock {
  /**
   * Returns the regular successor block.
   *
   * @return the regular successor block
   */
  @Pure
  @Nullable Block getRegularSuccessor();

  /**
   * Returns true if this block is empty (i.e., does not contain any contents).
   *
   * @return true if this block is empty
   */
  @Pure
  boolean isEmpty();
}
