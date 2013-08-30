package dataflow.cfg.block;

import dataflow.analysis.Store;

import javacutils.Pair;

/**
 * Implementation of a non-special basic block.
 *
 * @author Stefan Heule
 *
 */
public abstract class SingleSuccessorBlockImpl extends BlockImpl implements
        SingleSuccessorBlock {

    /** Internal representation of the successor. */
    protected /*@Nullable*/ BlockImpl successor;

    protected Pair<Store.Kind, Store.Kind> storeFlow;

    public SingleSuccessorBlockImpl() {
        storeFlow = Pair.of(Store.Kind.BOTH, Store.Kind.BOTH);
    }

    @Override
    public /*@Nullable*/ Block getSuccessor() {
        return successor;
    }

    /**
     * Set a basic block as the successor of this block.
     */
    public void setSuccessor(BlockImpl successor) {
        this.successor = successor;
        successor.addPredecessor(this);
    }

    @Override
    public Pair<Store.Kind, Store.Kind> getStoreFlow() {
        return storeFlow;
    }
}
