package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

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
    protected/* @Nullable */Node target;
    protected List<Node> arguments;

    public MethodInvocationNode(MethodInvocationTree tree,
            /* @Nullable */Node target,
            List<Node> arguments) {
        this.tree = tree;
        this.type = InternalUtils.typeOf(tree);
        this.target = target;
        this.arguments = arguments;
    }

    public/* @Nullable */Node getTarget() {
        return target;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public Node getArgument(int i) {
        return arguments.get(i);
    }

    @Override
    public Tree getTree() {
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
            sb.append("(" + target + ").");
        }
        sb.append(TreeUtils.elementFromUse(tree).getSimpleName());
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
        int hash = HashCodeUtils.hash(target);
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
