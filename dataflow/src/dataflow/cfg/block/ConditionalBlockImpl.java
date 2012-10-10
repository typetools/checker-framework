package checkers.flow.cfg.block;

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
     * Initialize an empty conditional basic block to be filled with contents
     * and linked to other basic blocks later.
     */
    public ConditionalBlockImpl() {
        type = BlockType.CONDITIONAL_BLOCK;
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
    public String toString() {
        return "ConditionalBlock()";
    }

}
