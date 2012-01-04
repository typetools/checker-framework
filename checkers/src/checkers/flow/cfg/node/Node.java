package checkers.flow.cfg.node;

import checkers.flow.cfg.block.Block;
import checkers.flow.util.NodeUtils;

import com.sun.source.tree.Tree;

/**
 * A node in the abstract representation used for Java code inside a basic
 * block.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class Node {

	/** The basic block this node belongs to. */
	protected/* @LazyNonNull */Block block;

	/** @return The basic block this node belongs to. */
	public Block getBlock() {
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
	 * <p>
	 * 
	 * <em>Important:</em> If this method returns <code>null</code>, then the
	 * node is not of a boolean type (cf. {@link NodeUtils.isBooleanTypeNode}).
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

}