package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree.Kind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

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
        assert tree.getKind() == Kind.CONDITIONAL_EXPRESSION;
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
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TernaryExpressionNode)) {
            return false;
        }
        TernaryExpressionNode other = (TernaryExpressionNode) obj;
        return getConditionOperand().equals(other.getConditionOperand())
                && getThenOperand().equals(other.getThenOperand())
                && getElseOperand().equals(other.getElseOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConditionOperand(), getThenOperand(), getElseOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        return Arrays.asList(getConditionOperand(), getThenOperand(), getElseOperand());
    }
}
