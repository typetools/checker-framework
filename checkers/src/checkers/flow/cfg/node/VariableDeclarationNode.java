package checkers.flow.cfg.node;

import checkers.flow.util.HashCodeUtils;

import com.sun.source.tree.VariableTree;

/**
 * A node for a local variable declaration:
 * 
 * <pre>
 *   <em>modifier</em> <em>type</em> <em>identifier</em>;
 * </pre>
 * 
 * Note: Does not have an initializer block, as that will be translated to a
 * separate {@link AssignmentNode}.
 * 
 * @author Stefan Heule
 * 
 */
public class VariableDeclarationNode extends Node {

	protected VariableTree tree;

	// TODO: make type and modifier accessible

	public VariableDeclarationNode(VariableTree t) {
		tree = t;
	}

	public String getName() {
		return tree.getName().toString();
	}

	@Override
	public VariableTree getTree() {
		return tree;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitVariableDeclaration(this, p);
	}

	@Override
	public String toString() {
		return getName().toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof VariableDeclarationNode)) {
			return false;
		}
		VariableDeclarationNode other = (VariableDeclarationNode) obj;
		return getName().equals(other.getName());
	}
	
	@Override
	public int hashCode() {
		return HashCodeUtils.hash(getName());
	}
}
