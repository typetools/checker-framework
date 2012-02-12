package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import com.sun.source.tree.Tree;
import com.sun.source.tree.LiteralTree;

/**
 * A node for a byte literal. For example:
 * 
 * <pre>
 *   <em>3</em>
 *   <em>0xff</em>
 * </pre>
 *
 * Java source and the AST representation do not have "byte" literals.
 * They have integer literals that may be narrowed to bytes depending
 * on context.  If we use explicit NarrowingConversionNodes, do we need
 * ByteLiteralNodes too?
 * TODO:  Decide this question.
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class ByteLiteralNode extends ValueLiteralNode {

	public ByteLiteralNode(LiteralTree t) {
		assert t.getKind().equals(Tree.Kind.INT_LITERAL);
		tree = t;
	}

	@Override
	public Byte getValue() {
		return (Byte) tree.getValue();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitByteLiteral(this, p);
	}
	
	@Override
	public boolean equals(Object obj) {
		// test that obj is a ByteLiteralNode
		if (obj == null || !(obj instanceof ByteLiteralNode)) {
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
}
