package checkers.flow.controlflowgraph;

import java.util.HashSet;
import java.util.Set;

import checkers.flow.controlflowgraph.node.Node;

/**
 * Implementation of the {@link BasicBlock} interface, representing a basic
 * block in a control flow graph that has a condition of an if statement as
 * contents.
 * 
 * @author Stefan Heule
 * 
 */
public class ConditionalBasicBlockImplementation extends
		BasicBlockImplementation implements ConditionalBasicBlock {

	/** Condition of the if statement. */
	private Node condition;

	/** Successor of the then branch. */
	private BasicBlock thenSuccessor;

	/** Successor of the else branch. */
	private BasicBlock elseSuccessor;

	/** Set of exceptional successors. */
	private Set<BasicBlock> exceptionalSuccessors;

	/**
	 * Initialize an empty basic block to be filled with contents and linked to
	 * other basic blocks later.
	 */
	public ConditionalBasicBlockImplementation() {
		exceptionalSuccessors = new HashSet<BasicBlock>();
	}

	/**
	 * Set the condition.
	 * TODO: remove if not needed
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
	 * Add an exceptional successor.
	 */
	void addExceptionalSuccessor(BasicBlock b) {
		exceptionalSuccessors.add(b);
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
		r.addAll(getExceptionalSuccessors());
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
	public Set<BasicBlock> getExceptionalSuccessors() {
		return new HashSet<BasicBlock>(exceptionalSuccessors);
	}

	@Override
	public Node getCondition() {
		return condition;
	}

}
