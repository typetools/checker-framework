package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;

/**
 * A node for the null literal.
 *
 * <pre>
 *   <em>null</em>
 * </pre>
 */
public class NullLiteralNode extends ValueLiteralNode {

    public NullLiteralNode(LiteralTree t) {
        super(t);
        assert t.getKind().equals(Tree.Kind.NULL_LITERAL);
    }

    @Override
    public Void getValue() {
        return (Void) tree.getValue();
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitNullLiteral(this, p);
    }

    @Override
    public boolean equals(Object obj) {
        // test that obj is a NullLiteralNode
        if (!(obj instanceof NullLiteralNode)) {
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
