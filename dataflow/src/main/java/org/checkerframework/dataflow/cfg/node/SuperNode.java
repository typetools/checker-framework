package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * A node for a reference to 'super'.
 *
 * <pre>
 *   <em>super</em>
 * </pre>
 */
public class SuperNode extends Node {

    protected final IdentifierTree tree;

    public SuperNode(IdentifierTree t) {
        super(TreeUtils.typeOf(t));
        assert t.getName().contentEquals("super");
        tree = t;
    }

    @Override
    public IdentifierTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitSuper(this, p);
    }

    @Override
    public String toString() {
        return "super";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof SuperNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash("super");
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
