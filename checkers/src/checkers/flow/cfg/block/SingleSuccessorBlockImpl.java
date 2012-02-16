package checkers.flow.cfg.block;

/**
 * Implementation of a non-special basic block.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class SingleSuccessorBlockImpl extends BlockImpl implements
        SingleSuccessorBlock {

    /** Internal representation of the successor. */
    protected/* @Nullable */BlockImpl successor;

    public SingleSuccessorBlockImpl() {
    }

    @Override
    public/* @Nullable */Block getSuccessor() {
        return successor;
    }

    /**
     * Set a basic block as the successor of this block.
     */
    public void setSuccessor(BlockImpl successor) {
        this.successor = successor;
        successor.addPredecessor(this);
    }

}
