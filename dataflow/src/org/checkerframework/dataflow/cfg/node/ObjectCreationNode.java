package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.NewClassTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for new object creation
 *
 * <pre>
 *   <em>new constructor(arg1, arg2, ...)</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 */
public class ObjectCreationNode extends Node {

    protected final NewClassTree tree;
    protected final Node constructor;
    protected final List<Node> arguments;

    public ObjectCreationNode(NewClassTree tree, Node constructor, List<Node> arguments) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.constructor = constructor;
        this.arguments = arguments;
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
        ArrayList<Node> list = new ArrayList<Node>(1 + arguments.size());
        list.add(constructor);
        list.addAll(arguments);
        return list;
    }
}
