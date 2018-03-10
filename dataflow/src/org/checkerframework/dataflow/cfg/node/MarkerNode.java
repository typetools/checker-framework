package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.util.HashCodeUtils;

/**
 * MarkerNodes are no-op Nodes used for debugging information. They can hold a Tree and a message,
 * which will be part of the String representation of the MarkerNode.
 *
 * <p>An example use case for MarkerNodes is representing switch statements.
 */
public class MarkerNode extends Node {

    protected final @Nullable Tree tree;
    protected final String message;

    public MarkerNode(@Nullable Tree tree, String message, Types types) {
        super(types.getNoType(TypeKind.NONE));
        this.tree = tree;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public @Nullable Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitMarker(this, p);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("marker (");
        sb.append(message);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MarkerNode)) {
            return false;
        }
        MarkerNode other = (MarkerNode) obj;
        if (tree == null && other.getTree() != null) {
            return false;
        }

        return getTree().equals(other.getTree()) && getMessage().equals(other.getMessage());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (tree != null) {
            hash = HashCodeUtils.hash(tree);
        }
        return HashCodeUtils.hash(hash, getMessage());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
