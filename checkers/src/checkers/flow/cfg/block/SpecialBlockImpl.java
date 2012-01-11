package checkers.flow.cfg.block;

public class SpecialBlockImpl extends SingleSuccessorBlockImpl implements SpecialBlock {

	/** The type of this special basic block. */
	protected SpecialBlockType specialType;
	
	public SpecialBlockImpl(SpecialBlockType type) {
		this.specialType = type;
		this.type = BlockType.SPECIAL_BLOCK;
	}

	@Override
	public SpecialBlockType getSpecialType() {
		return specialType;
	}

	/**
	 * Set a basic block as the successor of this block.
	 */
	public void setSuccessor(BlockImpl successor) {
		// setting the same successor twice is OK, as this is performed during
		// regular operation of the CFG to AST translation
		assert this.successor == null || this.successor == successor : "cannot set successor twice";
		this.successor = successor;
	}

	@Override
	public String toString() {
		return "SpecialBlock("+specialType+")";
	}

}
