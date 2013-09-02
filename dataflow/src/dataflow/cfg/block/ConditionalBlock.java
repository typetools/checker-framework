package dataflow.cfg.block;

import dataflow.analysis.Store;
import dataflow.cfg.node.Node;

/**
 * Represents a conditional basic block that contains exactly one boolean
 * {@link Node}.
 *
 * @author Stefan Heule
 *
 */
public interface ConditionalBlock extends Block {

    /**
     * @return The entry block of the then branch.
     */
    Block getThenSuccessor();

    /**
     * @return The entry block of the else branch.
     */
    Block getElseSuccessor();

    /**
     * @return The flow rule for information flowing from
     * this block to its then successor.
     */
    Store.FlowRule getThenStoreFlow();

    /**
     * @return The flow rule for information flowing from
     * this block to its else successor.
     */
    Store.FlowRule getElseStoreFlow();

    /**
     * Set the flow rule for information flowing from this block to
     * its then successor.
     */
    void setThenStoreFlow(Store.FlowRule rule);

    /**
     * Set the flow rule for information flowing from this block to
     * its else successor.
     */
    void setElseStoreFlow(Store.FlowRule rule);
}
