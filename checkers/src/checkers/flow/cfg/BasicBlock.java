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
public interface BasicBlock {

	/**
	 * @return The list of {@link Node}s that represent the contents of the
	 *         basic block.
	 */
	public List<Node> getContents();

	/**
	 * @return The list of regular (i.e., non-exceptional) successors.
	 */
	public BasicBlock getSuccessor();
	
	/**
	 * @return The list of exceptional successors.
	 */
	public Map<Class<?>, BasicBlock> getExceptionalSuccessors();

}
