package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store.FlowRule;

import java.util.Collections;
import java.util.Set;

/**
 * A basic block that has at most one successor. SpecialBlockImpl extends this, but exit blocks have
 * no successor.
 */
public abstract class SingleSuccessorBlockImpl extends BlockImpl implements SingleSuccessorBlock {

    /** Internal representation of the successor. */
    protected @Nullable BlockImpl successor;

    /**
     * The initial value for the rule below says that EACH store at the end of a single successor
     * block flows to the corresponding store of the successor.
     */
    protected FlowRule flowRule = FlowRule.EACH_TO_EACH;

    /**
     * Creates a new SingleSuccessorBlock.
     *
     * @param type the type of this basic block
     */
    protected SingleSuccessorBlockImpl(BlockType type) {
        super(type);
    }

    @Override
    public @Nullable Block getSuccessor() {
        return successor;
    }

    @Override
    public Set<Block> getSuccessors() {
        if (successor == null) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(successor);
        }
    }

    /**
     * Set a basic block as the successor of this block.
     *
     * @param successor the block that will be the successor of this
     */
    public void setSuccessor(BlockImpl successor) {
        this.successor = successor;
        successor.addPredecessor(this);
    }

    @Override
    public FlowRule getFlowRule() {
        return flowRule;
    }

    @Override
    public void setFlowRule(FlowRule rule) {
        flowRule = rule;
    }
}
