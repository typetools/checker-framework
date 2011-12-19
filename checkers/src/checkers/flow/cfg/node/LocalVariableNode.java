package checkers.flow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A node for local variables. For example:
 * <pre>
 *   <em>a</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class LocalVariableNode extends Node {

	protected Tree tree;

	public LocalVariableNode(Tree t) {
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

}
