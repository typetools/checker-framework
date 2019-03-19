package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for new array creation.
 *
 * <pre>
 *   <em>new type [1][2]</em>
 *   <em>new type [] = { expr1, expr2, ... }</em>
 * </pre>
 */
public class ArrayCreationNode extends Node {

    /** The tree is null when an array is created for variable arity method calls. */
    protected final @Nullable NewArrayTree tree;
    /**
     * The length of this list is the number of dimensions in the array. Each element is the size of
     * the given dimension.
     */
    protected final List<Node> dimensions;

    protected final List<Node> initializers;

    public ArrayCreationNode(
            @Nullable NewArrayTree tree,
            TypeMirror type,
            List<Node> dimensions,
            List<Node> initializers) {
        super(type);
        this.tree = tree;
        this.dimensions = dimensions;
        this.initializers = initializers;
    }

    public List<Node> getDimensions() {
        return dimensions;
    }

    public Node getDimension(int i) {
        return dimensions.get(i);
    }

    public List<Node> getInitializers() {
        return initializers;
    }

    public Node getInitializer(int i) {
        return initializers.get(i);
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitArrayCreation(this, p);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new " + type);
        if (!dimensions.isEmpty()) {
            boolean needComma = false;
            sb.append(" (");
            for (Node dim : dimensions) {
                if (needComma) {
                    sb.append(", ");
                }
                sb.append(dim);
                needComma = true;
            }
            sb.append(")");
        }
        if (!initializers.isEmpty()) {
            boolean needComma = false;
            sb.append(" = {");
            for (Node init : initializers) {
                if (needComma) {
                    sb.append(", ");
                }
                sb.append(init);
                needComma = true;
            }
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ArrayCreationNode)) {
            return false;
        }
        ArrayCreationNode other = (ArrayCreationNode) obj;

        return getDimensions().equals(other.getDimensions())
                && getInitializers().equals(other.getInitializers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensions, initializers);
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(dimensions.size() + initializers.size());
        list.addAll(dimensions);
        list.addAll(initializers);
        return list;
    }
}
