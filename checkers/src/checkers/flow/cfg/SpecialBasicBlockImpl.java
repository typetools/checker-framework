package checkers.flow.cfg;

import java.util.LinkedList;
import java.util.List;

import checkers.flow.cfg.node.Node;

/**
 * A <em>special</em> basic block that does not contain any contents and is used
 * to denote the entry or exit point of a method.
 * 
 * <p>
 * 
 * There are three different types of special basic blocks:
 * <ul>
 * <li><code>ENTRY</code> for the entry node of a method,</li>
 * <li><code>EXIT</code> for the exit node of a method,</li>
 * <li><code>EXCEPTIONAL_EXIT</code> for the exceptional exit node of a method.</li>
 * </ul>
 * 
 * @author Stefan Heule
 * 
 */
public class SpecialBasicBlockImpl extends BasicBlockImpl {

	/** The types of special basic blocks */
	public static enum SpecialBasicBlockTypes {

		/** The entry block of a method */
		ENTRY,

		/** The exit block of a method */
		EXIT,

		/** A special exit block of a method for exceptional termination */
		EXCEPTIONAL_EXIT,
	}

	/** Type of this special basic block */
	protected SpecialBasicBlockTypes type;

	/**
	 * Initialize a special basic block of a given type.
	 */
	public SpecialBasicBlockImpl(SpecialBasicBlockTypes type) {
		this.type = type;
	}

	/**
	 * @return The type of this basic block.
	 */
	public SpecialBasicBlockTypes getType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * 
	 * For special basic blocks, this method is guaranteed to return an empty
	 * list.
	 */
	@Override
	public List<Node> getContents() {
		return new LinkedList<Node>();
	}

	/**
	 * Note: not supported for special basic blocks.
	 */
	void addStatement(Node t) {
		assert false : "Special basic blocks do not have contents.";
	}

	/**
	 * Note: not supported for special basic blocks.
	 */
	public void addStatements(List<? extends Node> ts) {
		assert false : "Special basic blocks do not have contents.";
	}
	
	@Override
	public String toString() {
		return "SBB(type="+type+")";
	}

}
