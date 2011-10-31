package checkers.flow.controlflowgraph;

import java.util.List;
import java.util.Set;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;

/**
 * Represents a basic block in a control graph. Internally maintains a list of
 * {@link Tree}s that represent the contents of the basic block. This list can
 * contain of a list of statements (represented as {@link StatementTree}) or an
 * {@link ExpressionTree} in case of an if statement.
 * 
 * @author Stefan Heule
 * 
 * @see ConditionalBasicBlock
 * 
 */
public interface BasicBlock {

	/**
	 * Returns list of {@link Tree}s that represent the contents of the basic
	 * block. These can either be statements, or a single {@link ExpressionTree}
	 * in case of a {@link ConditionalBasicBlock}.
	 */
	public List<Tree> getContents();

	/**
	 * Returns a list of successors. An empty list indicates this basic block is
	 * the end of the control flow graph.
	 */
	public Set<BasicBlock> getSuccessors();

}
