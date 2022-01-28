package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ConditionalExpressionTree;

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

    /** The {@code ConditionalExpressionTree} corresponding to this node */
    protected final ConditionalExpressionTree tree;

    /** Node representing the condition checked by the expression */
    protected final Node condition;

    /** Node representing the "then" case of the expression */
    protected final Node thenOperand;

    /** Node representing the "else" case of the expression */
    protected final Node elseOperand;

    /**
     * This is a variable created by dataflow to which each case expression of the ternary
     * expression is assigned. Its value should be used for the value of the switch expression.
     */
    private final LocalVariableNode ternaryExpressionVar;

    /**
     * Creates a new TernaryExpressionNode.
     *
     * @param tree the {@code ConditionalExpressionTree} for the node
     * @param condition node representing the condition checked by the expression
     * @param thenOperand node representing the "then" case of the expression
     * @param elseOperand node representing the "else" case of the expression
     * @param ternaryExpressionVar a variable created by dataflow to which each case expression of
     *     the ternary expression is assigned. Its value should be used for the value of the switch
     *     expression.
     */
    public TernaryExpressionNode(
            ConditionalExpressionTree tree,
            Node condition,
            Node thenOperand,
            Node elseOperand,
            LocalVariableNode ternaryExpressionVar) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.condition = condition;
        this.thenOperand = thenOperand;
        this.elseOperand = elseOperand;
        this.ternaryExpressionVar = ternaryExpressionVar;
    }

    /**
     * Gets the node representing the conditional operand for this node
     *
     * @return the condition operand node
     */
    public Node getConditionOperand() {
        return condition;
    }

    /**
     * Gets the node representing the "then" operand for this node
     *
     * @return the "then" operand node
     */
    public Node getThenOperand() {
        return thenOperand;
    }

    /**
     * Gets the node representing the "else" operand for this node
     *
     * @return the "else" operand node
     */
    public Node getElseOperand() {
        return elseOperand;
    }

    /**
     * This is a variable created by dataflow to which each case expression of the ternary
     * expression is assigned. Its value should be used for the value of the switch expression.
     *
     * @return the variable for this ternary expression
     */
    public LocalVariableNode getTernaryExpressionVar() {
        return ternaryExpressionVar;
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
