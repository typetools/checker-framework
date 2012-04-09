package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;

import com.sun.source.tree.MethodInvocationTree;

/**
 * A node for the method invocation
 * 
 * <pre>
 *   <em>target.m(arg1, arg2, ...)</em>
 * </pre>
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class MethodInvocationNode extends Node {

    protected MethodInvocationTree tree;
    protected/* @Nullable */FieldAccessNode target;
    protected List<Node> arguments;

    public MethodInvocationNode(MethodInvocationTree tree,
            /* @Nullable */FieldAccessNode target,
            List<Node> arguments) {
        this.tree = tree;
        this.type = InternalUtils.typeOf(tree);
        this.target = target;
        this.arguments = arguments;
    }
    
    public/* @Nullable */FieldAccessNode getTarget() {
        return target;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public Node getArgument(int i) {
        return arguments.get(i);
    }

    @Override
    public MethodInvocationTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitMethodInvocation(this, p);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (target != null) {
            sb.append(target);
        }
        sb.append("(");
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
        if (obj == null || !(obj instanceof MethodInvocationNode)) {
            return false;
        }
        MethodInvocationNode other = (MethodInvocationNode) obj;
        if (target == null && other.getTarget() != null) {
            return false;
        }

        return getTarget().equals(other.getTarget())
                && getArguments().equals(other.getArguments());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (target != null) {
            hash = HashCodeUtils.hash(target);
        }
        for (Node arg : arguments) {
            hash = HashCodeUtils.hash(hash, arg.hashCode());
        }
        return hash;
    }

    @Override
    public Collection<Node> getOperands() {
        LinkedList<Node> list = new LinkedList<Node>();
        if (target != null) {
            list.add(target);
        }
        list.addAll(arguments);
        return list;
    }
}
