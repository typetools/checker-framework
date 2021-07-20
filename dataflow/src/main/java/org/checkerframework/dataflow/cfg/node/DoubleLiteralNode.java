package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A node for a double literal. For example:
 *
 * <pre>
 *   <em>-9.</em>
 *   <em>3.14159D</em>
 * </pre>
 */
public class DoubleLiteralNode extends ValueLiteralNode {

    /**
     * Create a new DoubleLiteralNode.
     *
     * @param t the tree for the literal value
     */
    public DoubleLiteralNode(LiteralTree t) {
        super(t);
        assert t.getKind() == Tree.Kind.DOUBLE_LITERAL;
    }

    @Override
    public Double getValue() {
        return (Double) tree.getValue();
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitDoubleLiteral(this, p);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // test that obj is a DoubleLiteralNode
        if (!(obj instanceof DoubleLiteralNode)) {
            return false;
        }
        // super method compares values
        return super.equals(obj);
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
