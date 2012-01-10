package checkers.flow.cfg.block;

import java.util.List;

import checkers.flow.cfg.node.Node;

/**
 * A regular basic block that contains a sequence of {@link Node}s.
 * 
 * @author Stefan Heule
 * 
 */
public interface RegularBlock extends SingleSuccessorBlock {

	/**
	 * Return the unmodifiable sequence of {@link Node}s. The following
	 * invariant holds.
	 * 
	 * <pre>
	 * forall n in getContents() :: n.getBlock() == this
	 * </pre>
	 * 
	 * @return The unmodifiable sequence of {@link Node}s.
	 */
	List<Node> getContents();

	/**
	 * @return The regular successor block.
	 */
	Block getRegularSuccessor();

}
