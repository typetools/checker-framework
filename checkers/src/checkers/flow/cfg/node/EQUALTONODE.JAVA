package checkers.flow.cfg.node;

import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for an equality check:
 * 
 * <pre>
 *   <em>expression</em> == <em>expression</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class EqualToNode extends Node {

	protected BinaryTree tree;
	protected Node lhs;
	protected Node rhs;

	public EqualToNode(BinaryTree tree, Node lhs, Node rhs) {
		assert tree.getKind().equals(Kind.EQUAL_TO);
		this.tree = tree;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Node getLeftOperand() {
		return lhs;
	}

	public Node getRightOperand() {
		return rhs;
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
		return visitor.visitEqualTo(this, p);
	}

	@Override
	public String toString() {
		return "(" + getLeftOperand() + " == " + getRightOperand() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof EqualToNode)) {
			return false;
		}
		EqualToNode other = (EqualToNode) obj;
		return getLeftOperand().equals(other.getLeftOperand())
				&& getRightOperand().equals(other.getRightOperand());
	}
	
	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getLeftOperand(), getRightOperand());
	}

}
