package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

/**
 * A node for an integer literal. For example:
 * 
 * <pre>
 *   <em>42</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class IntegerLiteralNode extends ValueLiteralNode {

	public IntegerLiteralNode(LiteralTree t) {
		assert t.getKind().equals(Tree.Kind.INT_LITERAL);
		tree = t;
	}

	@Override
	public Integer getValue() {
		return (Integer) tree.getValue();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitIntegerLiteral(this, p);
	}
	
	@Override
	public boolean equals(Object obj) {
		// test that obj is a IntegerLiteralNode
		if (obj == null || !(obj instanceof IntegerLiteralNode)) {
			return false;
		}
		// super method compares values
		return super.equals(obj);
	}

}
