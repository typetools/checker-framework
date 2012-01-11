package checkers.flow.cfg.block;


/**
 * Base class of the {@link Block} implementation hierarchy.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class BlockImpl implements Block {

	/** A unique ID for this node. */
	protected long id = BlockImpl.uniqueID();

	/** The last ID that has already been used. */
	protected static long lastId = 0;

	/** The type of this basic block. */
	protected BlockType type;

	/**
	 * @return A fresh identifier.
	 */
	private static long uniqueID() {
		return lastId++;
	}

	public BlockImpl() {
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public BlockType getType() {
		return type;
	}

}
