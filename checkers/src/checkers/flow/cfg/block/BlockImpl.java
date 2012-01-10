package checkers.flow.cfg.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class of the {@link Block} implementation hierarchy.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class BlockImpl implements Block {

	/** Set of exceptional successors (or null; lazily initialized). */
	protected/* @Nullable */Map<Class<? extends Throwable>, Block> exceptionalSuccessors;

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

	/**
	 * Add an exceptional successor.
	 */
	public void addExceptionalSuccessor(Block b,
			Class<? extends Throwable> cause) {
		if (exceptionalSuccessors == null) {
			exceptionalSuccessors = new HashMap<>();
		}
		exceptionalSuccessors.put(cause, b);
	}

	@Override
	public Map<Class<? extends Throwable>, Block> getExceptionalSuccessors() {
		if (exceptionalSuccessors == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(exceptionalSuccessors);
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
