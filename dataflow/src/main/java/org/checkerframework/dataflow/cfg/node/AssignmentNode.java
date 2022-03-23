package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A node for an assignment:
 *
 * <pre>
 *   <em>variable</em> = <em>expression</em>
 *   <em>variable</em> += <em>expression</em>
 *   <em>expression</em> . <em>field</em> = <em>expression</em>
 *   <em>expression</em> [ <em>index</em> ] = <em>expression</em>
 * </pre>
 *
 * We allow assignments without corresponding AST {@link Tree}s.
 *
 * <p>Some desugarings create additional assignments to synthetic local variables. Such assignment
 * nodes are marked as synthetic to allow special handling in transfer functions.
 *
 * <p>String concatenation compound assignments are desugared to an assignment and a string
 * concatenation.
 *
 * <p>Numeric compound assignments are desugared to an assignment and a numeric operation.
 */
public class AssignmentNode extends Node {

    /** The underlying assignment tree. */
    protected final Tree tree;
    /** The node for the LHS of the assignment tree. */
    protected final Node lhs;
    /** The node for the RHS of the assignment tree. */
    protected final Node rhs;

    /** Whether the assignment node is synthetic */
    protected final boolean synthetic;

    /**
     * Create a (non-synthetic) AssignmentNode.
     *
     * @param tree the {@code AssignmentTree} corresponding to the {@code AssignmentNode}
     * @param target the lhs of {@code tree}
     * @param expression the rhs of {@code tree}
     */
    public AssignmentNode(Tree tree, Node target, Node expression) {
        this(tree, target, expression, false);
    }

    /**
     * Create an AssignmentNode.
     *
     * @param tree the {@code AssignmentTree} corresponding to the {@code AssignmentNode}
     * @param target the lhs of {@code tree}
     * @param expression the rhs of {@code tree}
     * @param synthetic whether the assignment node is synthetic
     */
    public AssignmentNode(Tree tree, Node target, Node expression, boolean synthetic) {
        super(TreeUtils.typeOf(tree));
        assert tree instanceof AssignmentTree
                || tree instanceof VariableTree
                || tree instanceof CompoundAssignmentTree
                || tree instanceof UnaryTree;
        assert target instanceof FieldAccessNode
                || target instanceof LocalVariableNode
                || target instanceof ArrayAccessNode;
        this.tree = tree;
        this.lhs = target;
        this.rhs = expression;
        this.synthetic = synthetic;
    }

    /**
     * Returns the left-hand-side of the assignment.
     *
     * @return the left-hand-side of the assignment
     */
    public Node getTarget() {
        return lhs;
    }

    public Node getExpression() {
        return rhs;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    /**
     * Check if the assignment node is synthetic, e.g. the synthetic assignment in a ternary
     * expression.
     *
     * @return true if the assignment node is synthetic
     */
    public boolean isSynthetic() {
        return synthetic;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitAssignment(this, p);
    }

    @Override
    public String toString() {
        return getTarget() + " = " + getExpression() + (synthetic ? " (synthetic)" : "");
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof AssignmentNode)) {
            return false;
        }
        AssignmentNode other = (AssignmentNode) obj;
        return getTarget().equals(other.getTarget())
                && getExpression().equals(other.getExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTarget(), getExpression());
    }

    @Override
    public Collection<Node> getOperands() {
        return Arrays.asList(getTarget(), getExpression());
    }
}
