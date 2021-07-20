package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.AssignmentContext.MethodParameterContext;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.StringsPlume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A node for method invocation.
 *
 * <pre>
 *   <em>target(arg1, arg2, ...)</em>
 * </pre>
 *
 * CFGs may contain {@link MethodInvocationNode}s that correspond to no AST {@link Tree}, in which
 * case, the tree field will be null.
 */
public class MethodInvocationNode extends Node {

    /** The tree for the method invocation. */
    protected final @Nullable MethodInvocationTree tree;

    /**
     * The MethodAccessNode for the method being invoked. Includes the receiver if any. For a static
     * method, the receiver may be a class name.
     */
    protected final MethodAccessNode target;

    /** The arguments of the method invocation. */
    protected final List<Node> arguments;

    /** The tree path to the method invocation. */
    protected final TreePath treePath;

    /**
     * If this MethodInvocationNode is a node for an {@link Iterator#next()} desugared from an
     * enhanced for loop, then the {@code iterExpression} field is the expression in the for loop,
     * e.g., {@code iter} in {@code for(Object o: iter}.
     */
    protected @Nullable ExpressionTree iterableExpression;

    /**
     * Create a MethodInvocationNode.
     *
     * @param tree for the method invocation
     * @param target the MethodAccessNode for the method being invoked
     * @param arguments arguments of the method invocation
     * @param treePath path to the method invocation
     */
    public MethodInvocationNode(
            @Nullable MethodInvocationTree tree,
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

    /**
     * If this MethodInvocationNode is a node for an {@link Iterator#next()} desugared from an
     * enhanced for loop, then return the expression in the for loop, e.g., {@code iter} in {@code
     * for(Object o: iter}. Otherwise, return null.
     *
     * @return the iter expression, or null if this is not a {@link Iterator#next()} from an
     *     enhanced for loop
     */
    public @Nullable ExpressionTree getIterableExpression() {
        return iterableExpression;
    }

    /**
     * Set the iterable expression from a for loop.
     *
     * @param iterableExpression iterable expression
     * @see #getIterableExpression()
     */
    public void setIterableExpression(@Nullable ExpressionTree iterableExpression) {
        this.iterableExpression = iterableExpression;
    }

    @Override
    public @Nullable MethodInvocationTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitMethodInvocation(this, p);
    }

    @Override
    public String toString() {
        return target + "(" + StringsPlume.join(", ", arguments) + ")";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MethodInvocationNode)) {
            return false;
        }
        MethodInvocationNode other = (MethodInvocationNode) obj;

        return getTarget().equals(other.getTarget()) && getArguments().equals(other.getArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, arguments);
    }

    @Override
    public Collection<Node> getOperands() {
        List<Node> list = new ArrayList<>(1 + arguments.size());
        list.add(target);
        list.addAll(arguments);
        return list;
    }
}
