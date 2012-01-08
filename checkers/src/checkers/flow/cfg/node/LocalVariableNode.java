package checkers.flow.cfg.node;

import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A node for a local variable or a parameter:
 * 
 * <pre>
 *   <em>identifier</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class LocalVariableNode extends Node {

	protected Tree tree;

	public LocalVariableNode(Tree t) {
		// IdentifierTree for normal uses of the local variable or parameter,
		// and VariableTree for the translation of an initilizer block
		assert t instanceof IdentifierTree || t instanceof VariableTree;
		tree = t;
	}

	public String getName() {
		if (tree instanceof IdentifierTree) {
			return ((IdentifierTree) tree).getName().toString();
		}
		return ((VariableTree) tree).getName().toString();
	}

	@Override
	public Tree getTree() {
		return tree;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitIdentifier(this, p);
	}

	@Override
	public String toString() {
		return getName().toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof LocalVariableNode)) {
			return false;
		}
		LocalVariableNode other = (LocalVariableNode) obj;
		return getName().equals(other.getName());
	}
	
	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getName());
	}

}
