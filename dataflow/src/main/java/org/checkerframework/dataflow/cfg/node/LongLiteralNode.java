package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A node for a long literal. For example:
 *
 * <pre>
 *   <em>-3l</em>
 *   <em>0x80808080L</em>
 * </pre>
 */
public class LongLiteralNode extends ValueLiteralNode {

    /**
     * Create a new LongLiteralNode.
     *
     * @param t the tree for the literal value
     */
    public LongLiteralNode(LiteralTree t) {
        super(t);
        assert t.getKind() == Tree.Kind.LONG_LITERAL;
    }

    @Override
    public Long getValue() {
        return (Long) tree.getValue();
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitLongLiteral(this, p);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // test that obj is a LongLiteralNode
        if (!(obj instanceof LongLiteralNode)) {
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
