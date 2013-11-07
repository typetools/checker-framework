package dataflow.cfg.block;

/*>>>
import checkers.nullness.quals.Nullable;
*/

import dataflow.analysis.Store;

/**
 * A basic block that has at exactly one non-exceptional successor.
 *
 * @author Stefan Heule
 *
 */
public interface SingleSuccessorBlock extends Block {

    /**
     * @return The non-exceptional successor block, or {@code null} if there is
     *         no successor.
     */
    /*@Nullable*/ Block getSuccessor();

    /**
     * @return The flow rule for information flowing from this block to its successor.
     */
    Store.FlowRule getFlowRule();

    /**
     * Set the flow rule for information flowing from this block to its successor.
     */
    void setFlowRule(Store.FlowRule rule);
}
