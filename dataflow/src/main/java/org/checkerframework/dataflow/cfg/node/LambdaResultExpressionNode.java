package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TreeUtils;

/** A node for the single expression body of a single expression lambda */
public class LambdaResultExpressionNode extends Node {

    protected final ExpressionTree tree;
    protected final @Nullable Node result;

    public LambdaResultExpressionNode(ExpressionTree t, @Nullable Node result, Types types) {
        super(TreeUtils.typeOf(t));
        this.result = result;
        tree = t;
    }

    /**
     * Returns the final node of the CFG corresponding to the lambda expression body (see {@link
     * #getTree()}).
     */
    public Node getResult() {
        return result;
    }

    /**
     * Returns the {@link ExpressionTree} corresponding to the body of a lambda expression with an
     * expression body (e.g. X for (<code>o -&gt; X</code>) where X is an expression and not a {...}
     * block).
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
        if (result != null) {
            return "-> " + result;
        }
        return "-> ()";
    }

    @Override
    public boolean equals(Object obj) {
        // No need to compare tree, since in a well-formed LambdaResultExpressionNode, result will
        // be the same only when tree is the same (this is similar to ReturnNode).
        if (obj == null || !(obj instanceof LambdaResultExpressionNode)) {
            return false;
        }
        LambdaResultExpressionNode other = (LambdaResultExpressionNode) obj;
        if ((result == null) != (other.result == null)) {
            return false;
        }
        return (result == null || result.equals(other.result));
    }

    @Override
    public int hashCode() {
        // No need to incorporate tree, since in a well-formed LambdaResultExpressionNode, result
        // will be the same only when tree is the same (this is similar to ReturnNode).
        return HashCodeUtils.hash(result);
    }

    @Override
    public Collection<Node> getOperands() {
        if (result == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(result);
        }
    }
}
