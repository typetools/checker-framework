package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

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

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ImplicitThisLiteralNode)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return 17; // constant value, same as in ExplicitThisLiteralNode
	}

	@Override
	public Collection<Node> getOperands() {
		return Collections.emptyList();
	}
}
