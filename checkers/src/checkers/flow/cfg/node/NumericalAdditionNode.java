package checkers.flow.cfg.node;

import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for the numerical addition:
 * 
 * <pre>
 *   <em>expression</em> + <em>expression</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class NumericalAdditionNode extends Node {

	protected BinaryTree tree;
	protected Node left;
	protected Node right;

	public NumericalAdditionNode(BinaryTree tree, Node left, Node right) {
		assert tree.getKind().equals(Kind.PLUS);
		this.tree = tree;
		this.left = left;
		this.right = right;
	}

	public Node getLeftOperand() {
		return left;
	}

	public Node getRightOperand() {
		return right;
	}

	@Override
	public BinaryTree getTree() {
		return tree;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitNumericalAddition(this, p);
	}

	@Override
	public String toString() {
		return "(" + getLeftOperand() + " + " + getRightOperand() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof NumericalAdditionNode)) {
			return false;
		}
		NumericalAdditionNode other = (NumericalAdditionNode) obj;
		return getLeftOperand().equals(other.getLeftOperand())
				&& getRightOperand().equals(other.getRightOperand());
	}
	
	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getLeftOperand(), getRightOperand());
	}

}
