package checkers.flow.cfg.node;


import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

public class BooleanLiteralNode extends Node {
	
	protected LiteralTree tree;
	
	public BooleanLiteralNode(LiteralTree t) {
		assert t.getKind().equals(Tree.Kind.BOOLEAN_LITERAL);
		tree = t;
	}
	
	public LiteralTree getLiteralTree() {
		return tree;
	}
	
	public boolean getValue() {
		return (Boolean)tree.getValue();
	}

	@Override
	public Tree getTree() {
		return getLiteralTree();
	}

	@Override
	public
	<R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitBooleanLiteral(this, p);
	}
	
	@Override
	public String toString() {
		return Boolean.toString(getValue());
	}

}
