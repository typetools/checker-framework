package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.qual.Pure;

/** A basic block that has exactly one non-exceptional successor. */
public interface SingleSuccessorBlock extends Block {

  /**
   * Returns the non-exceptional successor block, or {@code null} if there is no non-exceptional
   * successor.
   *
   * @return the non-exceptional successor block, or {@code null} if there is no non-exceptional
   *     successor
   */
  @Pure
  @Nullable Block getSuccessor();

  /**
   * Returns the flow rule for information flowing from this block to its successor.
   *
   * @return the flow rule for information flowing from this block to its successor
   */
  @Pure
  FlowRule getFlowRule();

  /**
   * Set the flow rule for information flowing from this block to its successor.
   *
   * @param rule the new flow rule for information flowing from this block to its successor
   */
  void setFlowRule(FlowRule rule);
}
