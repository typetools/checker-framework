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
	 * @return The sequence of {@link Node}s.
	 */
	List<Node> getContents();
	
	/**
	 * @return The the regular successor block.
	 */
	Block getRegularSuccessor();

}
