package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.dataflow.analysis.Store.FlowRule;

// Werner believes that a ConditionalBlock has to have exactly one RegularBlock (?) predecessor and
// the last node of that predecessor has to be a node of boolean type. He's not totally sure,
// though.  We should check whether that property holds.

/** Represents a conditional basic block. */
public interface ConditionalBlock extends Block {

    /**
     * Returns the entry block of the then branch.
     *
     * @return the entry block of the then branch
     */
    Block getThenSuccessor();

    /**
     * Returns the entry block of the else branch.
     *
     * @return the entry block of the else branch
     */
    Block getElseSuccessor();

    /**
     * Returns the flow rule for information flowing from this block to its then successor.
     *
     * @return the flow rule for information flowing from this block to its then successor
     */
    FlowRule getThenFlowRule();

    /**
     * Returns the flow rule for information flowing from this block to its else successor.
     *
     * @return the flow rule for information flowing from this block to its else successor
     */
    FlowRule getElseFlowRule();

    /**
     * Set the flow rule for information flowing from this block to its then successor.
     *
     * @param rule the new flow rule for information flowing from this block to its then successor
     */
    void setThenFlowRule(FlowRule rule);

    /**
     * Set the flow rule for information flowing from this block to its else successor.
     *
     * @param rule the new flow rule for information flowing from this block to its else successor
     */
    void setElseFlowRule(FlowRule rule);
}
