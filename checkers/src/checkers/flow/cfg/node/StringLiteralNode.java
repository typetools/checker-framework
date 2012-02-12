package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

/**
 * A node for an string literal. For example:
 * 
 * <pre>
 *   <em>"abc"</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class StringLiteralNode extends ValueLiteralNode {

	public StringLiteralNode(LiteralTree t) {
		assert t.getKind().equals(Tree.Kind.STRING_LITERAL);
		tree = t;
	}

	@Override
	public String getValue() {
		return (String) tree.getValue();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitStringLiteral(this, p);
	}

	@Override
	public boolean equals(Object obj) {
		// test that obj is a StringLiteralNode
		if (obj == null || !(obj instanceof StringLiteralNode)) {
			return false;
		}
		// super method compares values
		return super.equals(obj);
	}

	@Override
	public Collection<Node> getOperands() {
		return Collections.emptyList();
	}

	@Override
	public boolean hasResult() {
		return true;
	}

	@Override
	public String toString() {
		return "\"" + super.toString() + "\"";
	}
}
