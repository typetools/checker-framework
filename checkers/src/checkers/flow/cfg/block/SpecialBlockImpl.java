package checkers.flow.cfg.block;

public class SpecialBlockImpl extends SingleSuccessorBlockImpl implements SpecialBlock {
	
	/** The type of this special basic block. */
	protected SpecialBlockType type;
	
	public SpecialBlockImpl(SpecialBlockType type) {
		this.type = type;
	}

	@Override
	public SpecialBlockType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return "SpecialBlock("+type+")";
	}

}
