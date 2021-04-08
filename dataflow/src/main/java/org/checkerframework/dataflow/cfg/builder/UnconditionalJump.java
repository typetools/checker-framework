package org.checkerframework.dataflow.cfg.builder;

import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;

/** An extended node of type {@link ExtendedNodeType#UNCONDITIONAL_JUMP}. */
class UnconditionalJump extends ExtendedNode {

  /** The jump target label. */
  protected final Label jumpTarget;

  /** The flow rule for this edge. */
  protected final FlowRule flowRule;

  /**
   * Construct an UnconditionalJump.
   *
   * @param jumpTarget the jump target label
   */
  public UnconditionalJump(Label jumpTarget) {
    this(jumpTarget, FlowRule.EACH_TO_EACH);
  }

  /**
   * Construct an UnconditionalJump, specifying its flow rule.
   *
   * @param jumpTarget the jump target label
   * @param flowRule the flow rule for this edge
   */
  public UnconditionalJump(Label jumpTarget, FlowRule flowRule) {
    super(ExtendedNodeType.UNCONDITIONAL_JUMP);
    assert jumpTarget != null;
    this.jumpTarget = jumpTarget;
    this.flowRule = flowRule;
  }

  @Override
  public Label getLabel() {
    return jumpTarget;
  }

  /**
   * Returns the flow rule for this edge.
   *
   * @return the flow rule for this edge
   */
  public FlowRule getFlowRule() {
    return flowRule;
  }

  /**
   * Produce a string representation.
   *
   * @return a string representation
   * @see org.checkerframework.dataflow.cfg.builder.CFGBuilder.PhaseOneResult#nodeToString
   */
  @Override
  public String toString() {
    return "JumpMarker(" + getLabel() + ")";
  }

  @Override
  public String toStringDebug() {
    return toString();
  }
}
