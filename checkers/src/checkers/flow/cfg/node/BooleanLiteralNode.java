package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

/**
 * A node for a boolean literal:
 * 
 * <pre>
 *   <em>true</em>
 *   <em>false</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class BooleanLiteralNode extends ValueLiteralNode {

	public BooleanLiteralNode(LiteralTree t) {
		assert t.getKind().equals(Tree.Kind.BOOLEAN_LITERAL);
		tree = t;
	}

	public LiteralTree getLiteralTree() {
		return tree;
	}

	@Override
	public Boolean getValue() {
		return (Boolean) tree.getValue();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitBooleanLiteral(this, p);
	}

}
