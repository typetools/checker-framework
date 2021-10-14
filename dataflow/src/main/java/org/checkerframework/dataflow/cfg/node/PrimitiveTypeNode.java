package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.PrimitiveTypeTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.lang.model.util.Types;

/**
 * A node representing a primitive type used in an expression such as a field access.
 *
 * <p><em>type</em> .class
 */
public class PrimitiveTypeNode extends Node {

    protected final PrimitiveTypeTree tree;

    /** For Types.isSameType. */
    protected final Types types;

    public PrimitiveTypeNode(PrimitiveTypeTree tree, Types types) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.types = types;
    }

    @Override
    public PrimitiveTypeTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitPrimitiveType(this, p);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof PrimitiveTypeNode)) {
            return false;
        }
        PrimitiveTypeNode other = (PrimitiveTypeNode) obj;
        return types.isSameType(getType(), other.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
