package org.checkerframework.dataflow.cfg.builder;

import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;

/**
 * An extended node of type {@link ExtendedNodeType#CONDITIONAL_JUMP}.
 *
 * <p><em>Important:</em> In the list of extended nodes, there should not be any labels that point
 * to a conditional jump. Furthermore, the node directly ahead of any conditional jump has to be a
 * {@link NodeWithExceptionsHolder} or {@link NodeHolder}, and the node held by that extended node
 * is required to be of boolean type.
 */
@SuppressWarnings("nullness") // TODO
class ConditionalJump extends ExtendedNode {

  /** The true successor label. */
  protected final Label trueSucc;
  /** The false successor label. */
  protected final Label falseSucc;

  /** The true branch flow rule. */
  protected FlowRule trueFlowRule;
  /** The false branch flow rule. */
  protected FlowRule falseFlowRule;

  /**
   * Construct a ConditionalJump.
   *
   * @param trueSucc true successor label
   * @param falseSucc false successor label
   */
  public ConditionalJump(Label trueSucc, Label falseSucc) {
    super(ExtendedNodeType.CONDITIONAL_JUMP);
    assert trueSucc != null;
    this.trueSucc = trueSucc;
    assert falseSucc != null;
    this.falseSucc = falseSucc;
  }

  public Label getThenLabel() {
    return trueSucc;
  }

  public Label getElseLabel() {
    return falseSucc;
  }

  /**
   * Returns the true branch flow rule.
   *
   * @return the true branch flow rule
   */
  public FlowRule getTrueFlowRule() {
    return trueFlowRule;
  }

  /**
   * Returns the false branch flow rule.
   *
   * @return the false branch flow rule
   */
  public FlowRule getFalseFlowRule() {
    return falseFlowRule;
  }

  /**
   * Sets the true branch flow rule.
   *
   * @param rule the new true branch flow rule
   */
  public void setTrueFlowRule(FlowRule rule) {
    trueFlowRule = rule;
  }

  /**
   * Sets the false branch flow rule.
   *
   * @param rule the new false branch flow rule
   */
  public void setFalseFlowRule(FlowRule rule) {
    falseFlowRule = rule;
  }

  /**
   * Produce a string representation.
   *
   * @return a string representation
   * @see org.checkerframework.dataflow.cfg.builder.CFGBuilder.PhaseOneResult#nodeToString
   */
  @Override
  public String toString() {
    return "TwoTargetConditionalJump(" + getThenLabel() + ", " + getElseLabel() + ")";
  }

  @Override
  public String toStringDebug() {
    return toString();
  }
}
