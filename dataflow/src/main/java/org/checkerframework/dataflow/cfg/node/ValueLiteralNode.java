package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LiteralTree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a literals that have some form of value:
 *
 * <ul>
 *   <li>integer literal
 *   <li>long literal
 *   <li>char literal
 *   <li>string literal
 *   <li>float literal
 *   <li>double literal
 *   <li>boolean literal
 *   <li>null literal
 * </ul>
 */
public abstract class ValueLiteralNode extends Node {

    /** The tree for the value literal. */
    protected final LiteralTree tree;

    /**
     * Returns the value of the literal, null for the null literal.
     *
     * @return the value of the literal, null for the null literal
     */
    public abstract @Nullable Object getValue();

    protected ValueLiteralNode(LiteralTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    @Override
    public LiteralTree getTree() {
        return tree;
    }

    @Override
    public String toString() {
        return String.valueOf(getValue());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValueLiteralNode)) {
            return false;
        }
        ValueLiteralNode other = (ValueLiteralNode) obj;
        Object val = getValue();
        Object otherVal = other.getValue();
        return Objects.equals(val, otherVal);
    }

    @Override
    public int hashCode() {
        // value might be null
        return Objects.hash(this.getClass(), getValue());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
