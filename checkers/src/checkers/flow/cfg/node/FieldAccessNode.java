package checkers.flow.cfg.node;

import checkers.flow.util.ASTUtils;

import com.sun.source.tree.Tree;

/**
 * A node for a field access. For example:
 * <pre>
 *   <em>receiver</em> . <em>f</em>
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
	
	public FieldAccessNode(Tree tree, Node receiver, String field) {
		assert ASTUtils.isFieldAccess(tree);
		this.tree = tree;
		this.receiver = receiver;
		this.field = field;
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

}
