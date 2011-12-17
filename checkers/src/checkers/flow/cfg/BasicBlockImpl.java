package checkers.flow.cfg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	/** Internal representation of the successors. */
	protected Set<BasicBlock> successors;
	
	/** Set of exceptional successors. */
	protected Map<Class<?>, BasicBlock> exceptionalSuccessors;

	/**
	 * Initialize an empty basic block to be filled with contents and linked to
	 * other basic blocks later.
	 */
	public BasicBlockImpl() {
		contents = new LinkedList<>();
		successors = new HashSet<>();
		exceptionalSuccessors = new HashMap<>();
	}
	
	/**
	 * Add an exceptional successor.
	 */
	void addExceptionalSuccessor(BasicBlock b, Class<?> cause) {
		exceptionalSuccessors.put(cause, b);
	}

	/**
	 * Add a basic block as the successor of this block.
	 */
	void addSuccessor(BasicBlock successor) {
		successors.add(successor);
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
	public void addStatements(Collection<? extends Node> ts) {
		contents.addAll(ts);
	}

	@Override
	public List<Node> getContents() {
		return new LinkedList<Node>(contents);
	}

	@Override
	public Set<BasicBlock> getSuccessors() {
		return new HashSet<BasicBlock>(successors);
	}
	
	@Override
	public Map<Class<?>, BasicBlock> getExceptionalSuccessors() {
		return new HashMap<>(exceptionalSuccessors);
	}

}
