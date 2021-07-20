package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Implementation of a conditional basic block. */
public class ConditionalBlockImpl extends BlockImpl implements ConditionalBlock {

    /** Successor of the then branch. */
    protected @Nullable BlockImpl thenSuccessor;

    /** Successor of the else branch. */
    protected @Nullable BlockImpl elseSuccessor;

    /**
     * The initial value says that the THEN store before a conditional block flows to BOTH of the
     * stores of the then successor.
     */
    protected FlowRule thenFlowRule = FlowRule.THEN_TO_BOTH;

    /**
     * The initial value says that the ELSE store before a conditional block flows to BOTH of the
     * stores of the else successor.
     */
    protected FlowRule elseFlowRule = FlowRule.ELSE_TO_BOTH;

    /**
     * Initialize an empty conditional basic block to be filled with contents and linked to other
     * basic blocks later.
     */
    public ConditionalBlockImpl() {
        super(BlockType.CONDITIONAL_BLOCK);
    }

    /** Set the then branch successor. */
    public void setThenSuccessor(BlockImpl b) {
        thenSuccessor = b;
        b.addPredecessor(this);
    }

    /** Set the else branch successor. */
    public void setElseSuccessor(BlockImpl b) {
        elseSuccessor = b;
        b.addPredecessor(this);
    }

    @Override
    public Block getThenSuccessor() {
        if (thenSuccessor == null) {
            throw new BugInCF(
                    "Requested thenSuccessor for conditional block before initialization");
        }
        return thenSuccessor;
    }

    @Override
    public Block getElseSuccessor() {
        if (elseSuccessor == null) {
            throw new BugInCF(
                    "Requested elseSuccessor for conditional block before initialization");
        }
        return elseSuccessor;
    }

    @Override
    public Set<Block> getSuccessors() {
        Set<Block> result = new LinkedHashSet<>(2);
        result.add(getThenSuccessor());
        result.add(getElseSuccessor());
        return result;
    }

    @Override
    public FlowRule getThenFlowRule() {
        return thenFlowRule;
    }

    @Override
    public FlowRule getElseFlowRule() {
        return elseFlowRule;
    }

    @Override
    public void setThenFlowRule(FlowRule rule) {
        thenFlowRule = rule;
    }

    @Override
    public void setElseFlowRule(FlowRule rule) {
        elseFlowRule = rule;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns an empty list.
     */
    @Override
    public List<Node> getNodes() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable Node getLastNode() {
        return null;
    }

    @Override
    public String toString() {
        return "ConditionalBlock()";
    }
}
