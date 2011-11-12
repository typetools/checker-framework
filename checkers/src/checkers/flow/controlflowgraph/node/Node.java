package checkers.flow.controlflowgraph.node;

import com.sun.source.tree.Tree;

/**
 * A node in the abstract representation used for Java code inside a basic
 * block.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class Node {

	abstract public Tree getTree();

	/**
	 * Accept method of the visitor pattern
	 * 
	 * @param <R>
	 *            Result type of the operation.
	 * @param <P>
	 *            Parameter type.
	 * @param visitor
	 *            The visitor to be applied to this node.
	 * @param p
	 *            The parameter for this operation.
	 */
	abstract <R, P> R accept(NodeVisitor<R, P> visitor, P p);

}