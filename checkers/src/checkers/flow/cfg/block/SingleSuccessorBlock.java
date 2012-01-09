package checkers.flow.cfg.block;


/**
 * A basic block that has at most one successor.
 * 
 * @author Stefan Heule
 * 
 */
public interface SingleSuccessorBlock extends Block {

	/**
	 * @return The successor block, or null.
	 */
	/* @Nullable */Block getSuccessor();

}
