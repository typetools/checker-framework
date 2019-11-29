package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.BugInCF;

/** Implementation of a conditional basic block. */
public class ConditionalBlockImpl extends BlockImpl implements ConditionalBlock {

    /** Successor of the then branch. */
    protected @Nullable BlockImpl thenSuccessor;

    /** Successor of the else branch. */
    protected @Nullable BlockImpl elseSuccessor;

    /**
     * The rules below say that the THEN store before a conditional block flows to BOTH of the
     * stores of the then successor, while the ELSE store before a conditional block flows to BOTH
     * of the stores of the else successor.
     */
    protected Store.FlowRule thenFlowRule = Store.FlowRule.THEN_TO_BOTH;

    protected Store.FlowRule elseFlowRule = Store.FlowRule.ELSE_TO_BOTH;

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
    public Store.FlowRule getThenFlowRule() {
        return thenFlowRule;
    }

    @Override
    public Store.FlowRule getElseFlowRule() {
        return elseFlowRule;
    }

    @Override
    public void setThenFlowRule(Store.FlowRule rule) {
        thenFlowRule = rule;
    }

    @Override
    public void setElseFlowRule(Store.FlowRule rule) {
        elseFlowRule = rule;
    }

    @Override
    public String toString() {
        return "ConditionalBlock()";
    }
}
