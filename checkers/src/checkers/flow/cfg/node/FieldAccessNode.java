package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import checkers.flow.util.ASTUtils;
import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.Tree;

/**
 * A node for a field access:
 * 
 * <pre>
 *   <em>expression</em> . <em>field</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class FieldAccessNode extends Node {

	protected Tree tree;
	protected String field;
	protected Node receiver;

	// TODO: add method to get modifiers (static, access level, ..)

	public FieldAccessNode(Tree tree, Node receiver) {
		assert ASTUtils.isFieldAccess(tree);
		this.tree = tree;
		this.receiver = receiver;
		this.field = ASTUtils.getFieldName(tree);
	}

	public Node getReceiver() {
		return receiver;
	}

	public String getFieldName() {
		return field;
	}

	@Override
	public Tree getTree() {
		return tree;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitFieldAccess(this, p);
	}

	@Override
	public String toString() {
		return getReceiver() + "." + field;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof FieldAccessNode)) {
			return false;
		}
		FieldAccessNode other = (FieldAccessNode) obj;
		return getReceiver().equals(other.getReceiver())
				&& getFieldName().equals(other.getFieldName());
	}

	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getReceiver(), getFieldName());
	}

	@Override
	public Collection<Node> getOperands() {
		return Collections.singletonList(receiver);
	}

	@Override
	public boolean hasResult() {
		return true;
	}
}
