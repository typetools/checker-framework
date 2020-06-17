package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;

/** Represents a conditional basic block that contains exactly one boolean {@link Node}. */
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
    Store.FlowRule getThenFlowRule();

    /**
     * Returns the flow rule for information flowing from this block to its else successor.
     *
     * @return the flow rule for information flowing from this block to its else successor
     */
    Store.FlowRule getElseFlowRule();

    /** Set the flow rule for information flowing from this block to its then successor. */
    void setThenFlowRule(Store.FlowRule rule);

    /** Set the flow rule for information flowing from this block to its else successor. */
    void setElseFlowRule(Store.FlowRule rule);
}
