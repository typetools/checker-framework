package checkers.flow.controlflowgraph.node;

import javax.lang.model.element.Name;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;


public class IdentifierNode extends Node {
	
	protected IdentifierTree tree;
	
	public IdentifierNode(IdentifierTree t) {
		tree = t;
	}
	
	public Name getName() {
		return tree.getName();
	}
	
	public IdentifierTree getIdentifierTree() {
		return tree;
	}

	@Override
	public Tree getTree() {
		return getIdentifierTree();
	}

	@Override
	public
	<R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitIdentifier(this, p);
	}
	
	@Override
	public String toString() {
		return getName().toString();
	}

}
