package checkers.flow.cfg.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import checkers.flow.cfg.node.Node;

/**
 * Base class of the {@link Block} implementation hierarchy.
 * 
 * @author Stefan Heule
 * 
 */
public class ExceptionBlockImpl extends SingleSuccessorBlockImpl implements ExceptionBlock {

	/** Set of exceptional successors. */
	protected Map<Class<? extends Throwable>, Block> exceptionalSuccessors;

	public ExceptionBlockImpl() {
		type = BlockType.EXCEPTION_BLOCK;
		exceptionalSuccessors = new HashMap<>();
	}

	/** The node of this block. */
	protected Node node;

	/**
	 * Set the node.
	 */
	public void setNode(Node c) {
		node = c;
		c.setBlock(this);
	}

	@Override
	public Node getNode() {
		return node;
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
	public String toString() {
		return "ExceptionBlock(" + node + ")";
	}

}
