package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

/**
 * A node for integer literals. For example:
 * 
 * <pre>
 *   <em>42</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class IntegerLiteralNode extends Node {

	protected LiteralTree tree;

	public IntegerLiteralNode(LiteralTree t) {
		assert t.getKind().equals(Tree.Kind.INT_LITERAL);
		tree = t;
	}

	public LiteralTree getLiteralTree() {
		return tree;
	}

	public int getValue() {
		return (Integer) tree.getValue();
	}

	@Override
	public Tree getTree() {
		return getLiteralTree();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitIntegerLiteral(this, p);
	}

	@Override
	public String toString() {
		return Integer.toString(getValue());
	}

}
