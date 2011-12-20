package checkers.flow.cfg.node;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A node for an assignment. For example:
 * <pre>
 *   <em>variable</em> = <em>expression</em>
 * </pre>
 * 
 * @author Stefan Heule
 * 
 */
public class AssignmentNode extends Node {

	protected Tree tree;
	protected Node lhs;
	protected Node rhs;
	
	public AssignmentNode(Tree tree, Node target, Node expression) {
		assert tree instanceof AssignmentTree || tree instanceof VariableTree;
		this.tree = tree;
		this.lhs = target;
		this.rhs = expression;
	}

	public Node getTarget() {
		return lhs;
	}
	
	public Node getExpression() {
		return rhs;
	}

	@Override
	public Tree getTree() {
		return tree;
	}

	@Override
	public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
		return visitor.visitAssignment(this, p);
	}
	
	@Override
	public String toString() {
		return getTarget() + " = " + getExpression();
	}

}
