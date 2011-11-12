package checkers.flow.controlflowgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.source.tree.Tree;

/**
 * Implementation of the {@link BasicBlock} interface, representing a basic
 * block in a control flow graph.
 * 
 * @author Stefan Heule
 * 
 */
public class BasicBlockImplementation implements BasicBlock {

	/** Internal representation of the contents. */
	private List<Tree> contents;

	/** Internal representation of the successors. */
	private Set<BasicBlock> successors;

	/**
	 * Initialize an empty basic block to be filled with contents and linked to
	 * other basic blocks later.
	 */
	public BasicBlockImplementation() {
		contents = new LinkedList<Tree>();
		successors = new HashSet<BasicBlock>();
	}

	@Override
	public List<Tree> getContents() {
		return new LinkedList<Tree>(contents);
	}

	@Override
	public Set<BasicBlock> getSuccessors() {
		return new HashSet<BasicBlock>(successors);
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
	void addStatement(Tree t) {
		contents.add(t);
	}
	
	/**
	 * Add multiple statements to the contents of this basic block.
	 */
	public void addStatements(Collection<? extends Tree> ts) {
		contents.addAll(ts);
	}

}
