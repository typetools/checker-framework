package org.checkerframework.dataflow.cfg.block;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;

/** Implementation of a non-special basic block. */
public abstract class SingleSuccessorBlockImpl extends BlockImpl implements SingleSuccessorBlock {

    /** Internal representation of the successor. */
    protected @Nullable BlockImpl successor;

    /**
     * The rule below say that EACH store at the end of a single successor block flow to the
     * corresponding store of the successor.
     */
    protected Store.FlowRule flowRule = Store.FlowRule.EACH_TO_EACH;

    protected SingleSuccessorBlockImpl(BlockType type) {
        super(type);
    }

    @Override
    public @Nullable Block getSuccessor() {
        return successor;
    }

    @Override
    public Set<Block> getSuccessors() {
        Set<Block> result = new LinkedHashSet<>();
        if (successor != null) {
            result.add(successor);
        }
        return result;
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
    public List<Node> getNodes() {
        return Collections.emptyList();
    }

    @Override
    public Store.FlowRule getFlowRule() {
        return flowRule;
    }

    @Override
    public void setFlowRule(Store.FlowRule rule) {
        flowRule = rule;
    }
}
