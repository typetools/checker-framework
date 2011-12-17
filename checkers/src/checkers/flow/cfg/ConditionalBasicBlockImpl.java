package checkers.flow.cfg;

import java.util.HashSet;
import java.util.Set;

import checkers.flow.cfg.node.Node;

/**
 * Implementation of the {@link BasicBlock} interface, representing a basic
 * block in a control flow graph that has a boolean expression as contents
 * (e.g., from an if-block).
 * 
 * @author Stefan Heule
 * 
 */
public class ConditionalBasicBlockImpl extends
		BasicBlockImpl implements ConditionalBasicBlock {

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
	public ConditionalBasicBlockImpl() {
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

	@Override
	void addSuccessor(BasicBlock successor) {
		assert false; // use set[Then/Else]Successor instead
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
