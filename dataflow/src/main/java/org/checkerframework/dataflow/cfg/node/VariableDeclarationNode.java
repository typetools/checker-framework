package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * A node for a variable declaration, including local variables and fields:
 *
 * <pre>
 *   <em>modifier</em> <em>type</em> <em>identifier</em>;
 * </pre>
 *
 * Note: Does not have an initializer block, as that will be translated to a separate {@link
 * AssignmentNode}.
 */
public class VariableDeclarationNode extends Node {

    protected final VariableTree tree;
    protected final String name;

    // TODO: make modifier accessible

    public VariableDeclarationNode(VariableTree t) {
        super(TreeUtils.typeOf(t));
        tree = t;
        name = tree.getName().toString();
    }

    public String getName() {
        return name;
    }

    @Override
    public VariableTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitVariableDeclaration(this, p);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof VariableDeclarationNode)) {
            return false;
        }
        VariableDeclarationNode other = (VariableDeclarationNode) obj;
        return getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
