package dataflow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import dataflow.util.HashCodeUtils;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for the prefix decrement operations:
 *
 * <pre>
 *   --<em>expression</em>
 * </pre>
 *
 * NOTE: If widening of the operand and narrowing of the result
 * are required, they are separate Nodes, so the decrement takes
 * place at the type of the operand.
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class PrefixDecrementNode extends Node {

    protected Tree tree;
    protected Node operand;

    public PrefixDecrementNode(Tree tree, Node operand) {
        super(operand.getType());
        assert tree.getKind() == Kind.PREFIX_DECREMENT;
        this.tree = tree;
        this.operand = operand;
    }

    public Node getOperand() {
        return operand;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitPrefixDecrement(this, p);
    }

    @Override
    public String toString() {
        return "(--" + getOperand() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PrefixDecrementNode)) {
            return false;
        }
        PrefixDecrementNode other = (PrefixDecrementNode) obj;
        return getOperand().equals(other.getOperand());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(getOperand());
    }
}
