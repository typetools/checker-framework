package checkers.flow.controlflowgraph.node;


import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

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
		return (Integer)tree.getValue();
	}

	@Override
	public Tree getTree() {
		return getLiteralTree();
	}

	@Override
	<R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitIntegerLiteral(this, p);
	}

}
