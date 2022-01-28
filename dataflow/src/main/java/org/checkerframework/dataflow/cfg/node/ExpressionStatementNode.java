package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * A node for an expression that is used as a statement.
 *
 * <pre>
 *   <em>expression</em>;
 * </pre>
 *
 * An {@code ExpressionStatementNode} appears in the CFG after the node(s) of the expression. The
 * node can be used for special-handling of expression statements or can be seen as a no-op marker
 * node.
 */
public class ExpressionStatementNode extends Node {
    /** The expression constituting this ExpressionStatementNode. */
    protected final ExpressionTree tree;

    /**
     * Construct a ExpressionStatementNode.
     *
     * @param t the expression constituting this ExpressionStatementNode
     */
    public ExpressionStatementNode(ExpressionTree t) {
        super(TreeUtils.typeOf(t));
        tree = t;
    }

    @Override
    public @Nullable Tree getTree() {
        return null;
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitExpressionStatement(this, p);
    }

    @Override
    public String toString() {
        return "expression statement " + tree.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }
}
