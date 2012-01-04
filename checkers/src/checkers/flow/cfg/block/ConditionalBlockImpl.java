package checkers.flow.cfg.block;

import checkers.flow.cfg.node.Node;

/**
 * Implementation of a conditional basic block.
 * 
 * @author Stefan Heule
 * 
 */
public class ConditionalBlockImpl extends BlockImpl implements
		ConditionalBlock {

	/** Condition of the if statement. */
	protected Node condition;

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
	 * Set the condition.
	 */
	public void setCondition(Node c) {
		condition = c;
		c.setBlock(this);
	}

	/**
	 * Set the then branch successor.
	 */
	public void setThenSuccessor(BlockImpl b) {
		thenSuccessor = b;
	}

	/**
	 * Set the else branch successor.
	 */
	public void setElseSuccessor(BlockImpl b) {
		elseSuccessor = b;
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
	public Node getCondition() {
		return condition;
	}

	@Override
	public String toString() {
		return "ConditionalBlock(cond=" + condition + ")";
	}

}
