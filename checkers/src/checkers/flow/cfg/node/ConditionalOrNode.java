package checkers.flow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;

/**
 * A node for a conditional or expression. For example:
 * <pre>
 *   <em>expression</em> || <em>expression</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class ConditionalOrNode extends Node {

	protected BinaryTree tree;
	protected Node lhs;
	protected Node rhs;
	protected Boolean truthValue;
	
	public ConditionalOrNode(BinaryTree tree, Node lhs, Node rhs, Boolean truthValue) {
		this.tree = tree;
		this.lhs = lhs;
		this.rhs = rhs;
		this.truthValue = truthValue;
	}

	public Node getLeftOperand() {
		return lhs;
	}
	
	public Node getRightOperand() {
		return rhs;
	}
	
	public Boolean getTruthValue() {
		return truthValue;
	}

	/**
	 * Guaranteed to return the same tree as {@link getTree}, but with a more
	 * specific type.
	 */
	public BinaryTree getBinaryTree() {
		return tree;
	}

	@Override
	public Tree getTree() {
		return getBinaryTree();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitConditionalOr(this, p);
	}
	
	@Override
	public String toString() {
		return "(" + getLeftOperand() + " || " + getRightOperand() + ")";
	}

}
