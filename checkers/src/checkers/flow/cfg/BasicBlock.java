package checkers.flow.cfg;

import java.util.List;
import java.util.Map;

import checkers.flow.cfg.node.Node;

/**
 * Represents a basic block in a control graph. Internally maintains a list of
 * {@link Node}s that represent the contents of the basic block.
 * 
 * @author Stefan Heule
 * 
 * @see ConditionalBasicBlock
 * 
 */
public abstract class BasicBlock {

	/**
	 * @return The list of {@link Node}s that represent the contents of the
	 *         basic block.
	 */
	abstract public List<Node> getContents();

	/**
	 * @return The regular (i.e., non-exceptional) successor.
	 */
	abstract BasicBlock getSuccessor();
	
	/**
	 * @return The list of exceptional successors.
	 */
	abstract Map<Class<? extends Throwable>, BasicBlock> getExceptionalSuccessors();
	
	/**
	 * @return The unique identifier of this node.
	 */
	public long getId() {
		return id;
	}

	/** A unique ID for this node. */
	protected long id = BasicBlock.uniqueID();

	/** The last ID that has already been used. */
	protected static long lastId = 0;

	/**
	 * @return A currently unused identifier.
	 */
	private static long uniqueID() {
		return lastId++;
	}

}
