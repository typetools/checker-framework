package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;
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

	/**
	 * Guaranteed to return the same tree as {@link getTree}, but with a more
	 * specific type.
	 */
	public VariableTree getVariableTree() {
		return tree;
	}

	@Override
	public Tree getTree() {
		return getVariableTree();
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitVariableDeclaration(this, p);
	}

	@Override
	public String toString() {
		return getName().toString();
	}

}
