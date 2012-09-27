package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.LinkedList;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * A node for an assignment:
 * 
 * <pre>
 *   <em>variable</em> = <em>expression</em>
 *   <em>expression</em> . <em>field</em> = <em>expression</em>
 *   <em>expression</em> [ <em>index</em> ] = <em>expression</em>
 * </pre>
 * 
 * We allow assignments without corresponding AST {@link Tree}s.
 *
 * @author Stefan Heule
 * 
 */
public class AssignmentNode extends Node {

    protected Tree tree;
    protected Node lhs;
    protected Node rhs;

    public AssignmentNode(Tree tree, Node target, Node expression) {
        super(InternalUtils.typeOf(tree));
        assert tree instanceof AssignmentTree || tree instanceof VariableTree
                || tree instanceof CompoundAssignmentTree;
        assert target instanceof FieldAccessNode
                || target instanceof LocalVariableNode
                || target instanceof ArrayAccessNode;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AssignmentNode)) {
            return false;
        }
        AssignmentNode other = (AssignmentNode) obj;
        return getTarget().equals(other.getTarget())
                && getExpression().equals(other.getExpression());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getTarget(), getExpression());
    }

    @Override
    public Collection<Node> getOperands() {
        LinkedList<Node> list = new LinkedList<Node>();
        list.add(getTarget());
        list.add(getExpression());
        return list;
    }
}
