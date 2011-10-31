package checkers.flow.controlflowgraph;

import java.util.HashSet;
import java.util.Set;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

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
	private ExpressionTree condition;

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
	 */
	void setCondition(ExpressionTree c) {
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
	 * For {@link ConditionalBasicBlock}, use setThenSuccessor, setElseSuccessor
	 * or addExceptionalSuccessor instead.
	 */
	@Override
	void addSuccessor(BasicBlock successor) {
		assert false : "setThenSuccessor, setElseSuccessor or addExceptionalSuccessor"
				+ " should be used instead of addSuccessor.";
	}

	/**
	 * For {@link ConditionalBasicBlock}, use setCondition instead.
	 */
	@Override
	void addStatement(Tree t) {
		assert false : "setCondition should be used instead of addStatement.";
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
	public ExpressionTree getCondition() {
		return condition;
	}

}
