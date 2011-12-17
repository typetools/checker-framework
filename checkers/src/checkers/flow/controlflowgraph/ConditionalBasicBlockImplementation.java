package checkers.flow.controlflowgraph;

import java.util.HashSet;
import java.util.Set;

import checkers.flow.controlflowgraph.node.Node;

/**
 * Implementation of the {@link BasicBlock} interface, representing a basic
 * block in a control flow graph that has a boolean expression as contents
 * (e.g., from an if-block).
 * 
 * @author Stefan Heule
 * 
 */
public class ConditionalBasicBlockImplementation extends
		BasicBlockImplementation implements ConditionalBasicBlock {

	/** Condition of the if statement. */
	protected Node condition;

	/** Successor of the then branch. */
	protected BasicBlock thenSuccessor;

	/** Successor of the else branch. */
	protected BasicBlock elseSuccessor;

	/**
	 * Initialize an empty basic block to be filled with contents and linked to
	 * other basic blocks later.
	 */
	public ConditionalBasicBlockImplementation() {
	}

	/**
	 * Set the condition. TODO: remove if not needed
	 */
	void setCondition(Node c) {
		condition = c;
	}

	/**
	 * Set the then branch successor.
	 */
	void setThenSuccessor(BasicBlock b) {
		thenSuccessor = b;
	}

	/**
	 * Set the else branch successor.
	 */
	void setElseSuccessor(BasicBlock b) {
		elseSuccessor = b;
	}

	/**
	 * Sets both the then and else successor of this node to
	 * <code>successor</code>
	 */
	@Override
	void addSuccessor(BasicBlock successor) {
		setThenSuccessor(successor);
		setElseSuccessor(successor);
	}

	@Override
	public Set<BasicBlock> getSuccessors() {
		Set<BasicBlock> r = new HashSet<BasicBlock>();
		r.add(thenSuccessor);
		r.add(elseSuccessor);
		return r;
	}

	@Override
	public BasicBlock getThenSuccessor() {
		return thenSuccessor;
	}

	@Override
	public BasicBlock getElseSuccessor() {
		return elseSuccessor;
	}

	@Override
	public Node getCondition() {
		return condition;
	}

}
