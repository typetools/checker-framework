package org.checkerframework.dataflow.cfg.builder;

import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;

/**
 * An extended node of type {@link ExtendedNodeType#CONDITIONAL_JUMP}.
 *
 * <p><em>Important:</em> In the list of extended nodes, there should not be any labels that point
 * to a conditional jump. Furthermore, the node directly ahead of any conditional jump has to be a
 * {@link NodeWithExceptionsHolder} or {@link NodeHolder}, and the node held by that extended node
 * is required to be of boolean type.
 */
class ConditionalJump extends ExtendedNode {

    /** The true successor label. */
    protected final Label trueSucc;
    /** The false successor label. */
    protected final Label falseSucc;

    /** The true branch flow rule. */
    protected Store.FlowRule trueFlowRule;
    /** The false branch flow rule. */
    protected Store.FlowRule falseFlowRule;

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

    public Store.FlowRule getTrueFlowRule() {
        return trueFlowRule;
    }

    public Store.FlowRule getFalseFlowRule() {
        return falseFlowRule;
    }

    public void setTrueFlowRule(Store.FlowRule rule) {
        trueFlowRule = rule;
    }

    public void setFalseFlowRule(Store.FlowRule rule) {
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
