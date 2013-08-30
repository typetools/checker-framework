package dataflow.cfg.block;

import dataflow.analysis.Store;

import javacutils.Pair;

/**
 * Implementation of a conditional basic block.
 *
 * @author Stefan Heule
 *
 */
public class ConditionalBlockImpl extends BlockImpl implements ConditionalBlock {

    /** Successor of the then branch. */
    protected BlockImpl thenSuccessor;

    /** Successor of the else branch. */
    protected BlockImpl elseSuccessor;

    /**
     * A store flow contains the kinds of destination stores
     * to which a store flows.
     */
    protected Pair<Store.Kind, Store.Kind> thenStoreFlow;
    
    protected Pair<Store.Kind, Store.Kind> elseStoreFlow;

    /**
     * Initialize an empty conditional basic block to be filled with contents
     * and linked to other basic blocks later.
     */
    public ConditionalBlockImpl() {
        type = BlockType.CONDITIONAL_BLOCK;
        thenStoreFlow = Pair.of(Store.Kind.THEN, Store.Kind.BOTH);
        elseStoreFlow = Pair.of(Store.Kind.ELSE, Store.Kind.BOTH);
    }

    /**
     * Set the then branch successor.
     */
    public void setThenSuccessor(BlockImpl b) {
        thenSuccessor = b;
        b.addPredecessor(this);
    }

    /**
     * Set the else branch successor.
     */
    public void setElseSuccessor(BlockImpl b) {
        elseSuccessor = b;
        b.addPredecessor(this);
    }

    @Override
    public Block getThenSuccessor() {
        return thenSuccessor;
    }

    @Override
    public Block getElseSuccessor() {
        return elseSuccessor;
    }

     @Override
    public Pair<Store.Kind, Store.Kind> getThenStoreFlow() {
        return thenStoreFlow;
    }

    @Override
    public Pair<Store.Kind, Store.Kind> getElseStoreFlow() {
        return elseStoreFlow;
    }

    @Override
    public String toString() {
        return "ConditionalBlock()";
    }

}
