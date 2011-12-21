package checkers.flow.cfg;

import checkers.flow.cfg.node.Node;

/**
 * Implementation of the {@link BasicBlock} interface, representing a basic
 * block in a control flow graph that has a boolean expression as contents
 * (e.g., from an if-block).
 * 
 * @author Stefan Heule
 * 
 */
public class ConditionalBasicBlockImpl extends BasicBlockImpl {

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
	void setSuccessor(BasicBlock successor) {
		assert false; // use set[Then/Else]Successor instead
	}

	@Override
	public BasicBlock getSuccessor() {
		assert false : "use getThenSuccessor or getElseSuccessor instead";
		return null;
	}

	/**
	 * @return The entry block of the then branch.
	 */
	public BasicBlock getThenSuccessor() {
		return thenSuccessor;
	}

	/**
	 * @return The entry block of the else branch.
	 */
	public BasicBlock getElseSuccessor() {
		return elseSuccessor;
	}

	/**
	 * @return The condition of the if statement.
	 */
	public Node getCondition() {
		return condition;
	}

	@Override
	public String toString() {
		return "CBB(cond=" + condition + ")";
	}

}
