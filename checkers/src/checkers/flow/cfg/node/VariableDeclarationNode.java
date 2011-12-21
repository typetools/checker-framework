package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A node for a local variable declaration. For example:
 * <pre>
 *   <em>modifier</em> <em>type</em>  <em>var</em>;
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class VariableDeclarationNode extends Node {
	
	protected VariableTree tree;
	
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
	public
	<R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitVariableDeclaration(this, p);
	}
	
	@Override
	public String toString() {
		return getName().toString();
	}

}
