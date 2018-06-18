package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for new object creation
 *
 * <pre>
 *   <em>new constructor(arg1, arg2, ...)</em>
 * </pre>
 */
public class ObjectCreationNode extends Node {

    protected final NewClassTree tree;
    protected final Node constructor;
    protected final List<Node> arguments;

    // Class body for anonymous classes, otherwise null.
    protected final @Nullable ClassDeclarationNode classbody;

    public ObjectCreationNode(
            NewClassTree tree,
            Node constructor,
            List<Node> arguments,
            @Nullable ClassDeclarationNode classbody) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.constructor = constructor;
        this.arguments = arguments;
        this.classbody = classbody;
    }

    public Node getConstructor() {
        return constructor;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public Node getArgument(int i) {
        return arguments.get(i);
    }

    public @Nullable Node getClassBody() {
        return classbody;
    }

    @Override
    public NewClassTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitObjectCreation(this, p);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new " + constructor + "(");
        boolean needComma = false;
        for (Node arg : arguments) {
            if (needComma) {
                sb.append(", ");
            }
            sb.append(arg);
            needComma = true;
        }
        sb.append(")");
        if (classbody != null) {
            // TODO: maybe this can be done nicer...
            sb.append(" ");
            sb.append(classbody.toString());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ObjectCreationNode)) {
            return false;
        }
        ObjectCreationNode other = (ObjectCreationNode) obj;
        if (constructor == null && other.getConstructor() != null) {
            return false;
        }

        return getConstructor().equals(other.getConstructor())
                && getArguments().equals(other.getArguments());
    }

    @Override
    public int hashCode() {
        int hash = HashCodeUtils.hash(constructor);
        for (Node arg : arguments) {
            hash = HashCodeUtils.hash(hash, arg.hashCode());
        }
        return hash;
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(1 + arguments.size());
        list.add(constructor);
        list.addAll(arguments);
        return list;
    }
}
