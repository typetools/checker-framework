package checkers.flow.cfg.node;

import java.util.Collection;

import checkers.flow.cfg.block.Block;

import com.sun.source.tree.Tree;

/**
 * A node in the abstract representation used for Java code inside a basic
 * block.
 * 
 * <p>
 * 
 * The following invariants hold:
 * 
 * <pre>
 * block == null || block instanceof RegularBlock || block instanceof ExceptionBlock
 * block instanceof RegularBlock ==> block.getContents().contains(this)
 * block instanceof ExceptionBlock ==> block.getNode() == this
 * block == null <==> "This object represents a parameter of the method."
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public abstract class Node {

	/**
	 * The basic block this node belongs to (see invariant about this field
	 * above).
	 */
	protected/* @Nullable */Block block;

	/**
	 * @return The basic block this node belongs to (or {@code null} if it
	 *         represents the parameter of a method).
	 */
	public/* @Nullable */Block getBlock() {
		return block;
	}

	/** Set the basic block this node belongs to. */
	public void setBlock(Block b) {
		block = b;
	}

	/**
	 * Returns the {@link Tree} in the abstract synatx tree, or
	 * <code>null</code> if no corresponding tree exists. For instance, this is
	 * the case for an {@link ImplicitThisLiteralNode}.
	 * 
	 * @return The corresponding {@link Tree} or <code>null</code>.
	 */
	abstract public/* @Nullable */Tree getTree();

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
	public abstract <R, P> R accept(NodeVisitor<R, P> visitor, P p);

	/**
	 * @return a collection containing all of the operand {@link Node}s
	 *         of this {@link Node}.
	 */
	public abstract Collection<Node> getOperands();

    /**
     * @return true iff this {@link Node} produces a result value
     */
    public boolean hasResult() { return false; }
}