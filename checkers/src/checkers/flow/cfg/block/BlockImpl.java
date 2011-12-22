package checkers.flow.cfg.block;

import java.util.HashMap;
import java.util.Map;


/**
 * Base class of the {@link Block} implementation hierarchy.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class BlockImpl implements Block {
	
	/** Set of exceptional successors. */
	protected Map<Class<? extends Throwable>, Block> exceptionalSuccessors;

	/** A unique ID for this node. */
	protected long id = BlockImpl.uniqueID();

	/** The last ID that has already been used. */
	protected static long lastId = 0;

	/**
	 * @return A currently unused identifier.
	 */
	private static long uniqueID() {
		return lastId++;
	}
	
	public BlockImpl() {
		exceptionalSuccessors = new HashMap<>();
	}
	
	/**
	 * Add an exceptional successor.
	 */
	public void addExceptionalSuccessor(Block b, Class<? extends Throwable> cause) {
		exceptionalSuccessors.put(cause, b);
	}
	
	@Override
	public Map<Class<? extends Throwable>, Block> getExceptionalSuccessors() {
		return new HashMap<>(exceptionalSuccessors);
	}
	
	@Override
	public long getId() {
		return id;
	}

}
