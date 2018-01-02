package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.MethodParameterContext;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for method invocation
 *
 * <pre>
 *   <em>target(arg1, arg2, ...)</em>
 * </pre>
 *
 * CFGs may contain {@link MethodInvocationNode}s that correspond to no AST {@link Tree}, in which
 * case, the tree field will be null.
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 */
public class MethodInvocationNode extends Node {

    protected final MethodInvocationTree tree;
    protected final MethodAccessNode target;
    protected final List<Node> arguments;
    protected final TreePath treePath;

    public MethodInvocationNode(
            MethodInvocationTree tree,
            MethodAccessNode target,
            List<Node> arguments,
            TreePath treePath) {
        super(tree != null ? TreeUtils.typeOf(tree) : target.getMethod().getReturnType());
        this.tree = tree;
        this.target = target;
        this.arguments = arguments;
        this.treePath = treePath;

        // set assignment contexts for parameters
        int i = 0;
        for (Node arg : arguments) {
            AssignmentContext ctx = new MethodParameterContext(target.getMethod(), i++);
            arg.setAssignmentContext(ctx);
        }
    }

    public MethodInvocationNode(MethodAccessNode target, List<Node> arguments, TreePath treePath) {
        this(null, target, arguments, treePath);
    }

    public MethodAccessNode getTarget() {
        return target;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public Node getArgument(int i) {
        return arguments.get(i);
    }

    public TreePath getTreePath() {
        return treePath;
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
        StringBuilder sb = new StringBuilder();
        sb.append(target);
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

        return getTarget().equals(other.getTarget()) && getArguments().equals(other.getArguments());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = HashCodeUtils.hash(target);
        for (Node arg : arguments) {
            hash = HashCodeUtils.hash(hash, arg.hashCode());
        }
        return hash;
    }

    @Override
    public Collection<Node> getOperands() {
        List<Node> list = new ArrayList<Node>(1 + arguments.size());
        list.add(target);
        list.addAll(arguments);
        return list;
    }
}
