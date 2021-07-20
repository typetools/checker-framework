package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A node for a character literal. For example:
 *
 * <pre>
 *   <em>'a'</em>
 *   <em>'\t'</em>
 *   <em>'\u03a9'</em>
 * </pre>
 */
public class CharacterLiteralNode extends ValueLiteralNode {

    /**
     * Create a new CharacterLiteralNode.
     *
     * @param t the character literal
     */
    public CharacterLiteralNode(LiteralTree t) {
        super(t);
        assert t.getKind() == Tree.Kind.CHAR_LITERAL;
    }

    @Override
    public Character getValue() {
        return (Character) tree.getValue();
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitCharacterLiteral(this, p);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // test that obj is a CharacterLiteralNode
        if (!(obj instanceof CharacterLiteralNode)) {
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
