package checkers.flow.controlflowgraph;

import java.util.List;
import java.util.Set;

import checkers.flow.controlflowgraph.node.Node;

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
	 * Returns list of {@link Node}s that represent the contents of the basic
	 * block.
	 */
	public List<Node> getContents();

	/**
	 * Returns a list of successors. An empty list indicates this basic block is
	 * the end of the control flow graph.
	 */
	public Set<BasicBlock> getSuccessors();

}
