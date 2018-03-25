package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a conditional expression:
 *
 * <pre>
 *   <em>expression</em> ? <em>expression</em> : <em>expression</em>
 * </pre>
 */
public class TernaryExpressionNode extends Node {

    protected final ConditionalExpressionTree tree;
    protected final Node condition;
    protected final Node thenOperand;
    protected final Node elseOperand;

    public TernaryExpressionNode(
            ConditionalExpressionTree tree, Node condition, Node thenOperand, Node elseOperand) {
        super(TreeUtils.typeOf(tree));
        assert tree.getKind().equals(Kind.CONDITIONAL_EXPRESSION);
        this.tree = tree;
        this.condition = condition;
        this.thenOperand = thenOperand;
        this.elseOperand = elseOperand;
    }

    public Node getConditionOperand() {
        return condition;
    }

    public Node getThenOperand() {
        return thenOperand;
    }

    public Node getElseOperand() {
        return elseOperand;
    }

    @Override
    public ConditionalExpressionTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitTernaryExpression(this, p);
    }

    @Override
    public String toString() {
        return "("
                + getConditionOperand()
                + " ? "
                + getThenOperand()
                + " : "
                + getElseOperand()
                + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TernaryExpressionNode)) {
            return false;
        }
        TernaryExpressionNode other = (TernaryExpressionNode) obj;
        return getConditionOperand().equals(other.getConditionOperand())
                && getThenOperand().equals(other.getThenOperand())
                && getElseOperand().equals(other.getElseOperand());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getConditionOperand(), getThenOperand(), getElseOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(3);
        list.add(getConditionOperand());
        list.add(getThenOperand());
        list.add(getElseOperand());
        return list;
    }
}
