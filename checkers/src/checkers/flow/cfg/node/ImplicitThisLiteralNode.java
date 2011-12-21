package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;

/**
 * A node to model the implicit <code>this</code>, e.g., in a field access.
 * 
 * @author Stefan Heule
 * 
 */
public class ImplicitThisLiteralNode extends Node {

	public ImplicitThisLiteralNode() {
	}

	@Override
	public Tree getTree() {
		return null;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitImplicitThisLiteral(this, p);
	}

	@Override
	public String toString() {
		return "(this)";
	}

}
