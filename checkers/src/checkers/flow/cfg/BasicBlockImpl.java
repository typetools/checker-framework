package checkers.flow.cfg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import checkers.flow.cfg.node.Node;

/**
 * Implementation of the {@link BasicBlock} interface, representing a basic
 * block in a control flow graph.
 * 
 * @author Stefan Heule
 * 
 */
public class BasicBlockImpl implements BasicBlock {

	/** Internal representation of the contents. */
	protected List<Node> contents;

	/** Internal representation of the successor. */
	protected BasicBlock successor;

	/** Set of exceptional successors. */
	protected Map<Class<? extends Throwable>, BasicBlock> exceptionalSuccessors;

	/**
	 * Initialize an empty basic block to be filled with contents and linked to
	 * other basic blocks later.
	 */
	public BasicBlockImpl() {
		contents = new LinkedList<>();
		exceptionalSuccessors = new HashMap<>();
	}

	/**
	 * Add an exceptional successor.
	 */
	void addExceptionalSuccessor(BasicBlock b, Class<? extends Throwable> cause) {
		exceptionalSuccessors.put(cause, b);
	}

	/**
	 * Add a basic block as the successor of this block.
	 */
	void setSuccessor(BasicBlock successor) {
		// setting the same successor twice is OK, as this is performed during
		// regular operation of the CFG to AST translation
		assert this.successor == null || this.successor == successor : "cannot set successor twice";
		this.successor = successor;
	}

	/**
	 * Add a statement to the contents of this basic block.
	 */
	void addStatement(Node t) {
		contents.add(t);
	}

	/**
	 * Add multiple statements to the contents of this basic block.
	 */
	public void addStatements(List<? extends Node> ts) {
		contents.addAll(ts);
	}

	@Override
	public List<Node> getContents() {
		return new LinkedList<Node>(contents);
	}

	@Override
	public BasicBlock getSuccessor() {
		return successor;
	}

	@Override
	public Map<Class<? extends Throwable>, BasicBlock> getExceptionalSuccessors() {
		return new HashMap<>(exceptionalSuccessors);
	}

	@Override
	public String toString() {
		return "BB(" + contents + ")";
	}

}
