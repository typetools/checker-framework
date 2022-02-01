package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/** A node for the single expression body of a single expression lambda. */
public class LambdaResultExpressionNode extends Node {

    /** Tree for the lambda expression body. */
    protected final ExpressionTree tree;

    /** Final CFG node corresponding to the lambda expression body. */
    protected final Node result;

    /**
     * Creates a LambdaResultExpressionNode.
     *
     * @param t tree for the lambda expression body
     * @param result final CFG node corresponding to the lambda expression body
     */
    public LambdaResultExpressionNode(ExpressionTree t, Node result) {
        super(TreeUtils.typeOf(t));
        this.result = result;
        tree = t;
    }

    /**
     * Returns the final node of the CFG corresponding to the lambda expression body (see {@link
     * #getTree()}).
     *
     * @return the final node of the CFG corresponding to the lambda expression body
     */
    public Node getResult() {
        return result;
    }

    /**
     * Returns the {@link ExpressionTree} corresponding to the body of a lambda expression with an
     * expression body (e.g. X for ({@code o -> X}) where X is an expression and not a {...} block).
     *
     * @return the {@link ExpressionTree} corresponding to the body of a lambda expression with an
     *     expression body
     */
    @Override
    public ExpressionTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitLambdaResultExpression(this, p);
    }

    @Override
    public String toString() {
        return "-> " + result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // No need to compare tree, since in a well-formed LambdaResultExpressionNode, result will
        // be the same only when tree is the same (this is similar to ReturnNode).
        if (!(obj instanceof LambdaResultExpressionNode)) {
            return false;
        }
        LambdaResultExpressionNode other = (LambdaResultExpressionNode) obj;
        return Objects.equals(result, other.result);
    }

    @Override
    public int hashCode() {
        // No need to incorporate tree, since in a well-formed LambdaResultExpressionNode, result
        // will be the same only when tree is the same (this is similar to ReturnNode).
        return Objects.hash(LambdaResultExpressionNode.class, result);
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(result);
    }
}
