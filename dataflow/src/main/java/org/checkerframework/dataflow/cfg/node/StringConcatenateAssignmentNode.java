package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A node for the string concatenation compound assignment:
 *
 * <pre>
 *   <em>variable</em> += <em>expression</em>
 * </pre>
 *
 * @deprecated StringConcatenateAssignmentNode is no longer used in CFGs. Instead, an assignment and
 *     a concatenation node are generated.
 */
@Deprecated // 2022-03-22
public class StringConcatenateAssignmentNode extends Node {
    /** The entire tree of the assignment */
    protected final Tree tree;
    /** The left-hand side of the assignment */
    protected final Node left;
    /** The right-hand side of the assignment */
    protected final Node right;

    /**
     * Constructs an {@link StringConcatenateAssignmentNode}.
     *
     * @param tree the binary tree of the assignment
     * @param left the left-hand side
     * @param right the right-hand side
     */
    public StringConcatenateAssignmentNode(Tree tree, Node left, Node right) {
        super(TreeUtils.typeOf(tree));
        assert tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT;
        this.tree = tree;
        this.left = left;
        this.right = right;
    }

    public Node getLeftOperand() {
        return left;
    }

    public Node getRightOperand() {
        return right;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitStringConcatenateAssignment(this, p);
    }

    @Override
    public Collection<Node> getOperands() {
        return Arrays.asList(getLeftOperand(), getRightOperand());
    }

    @Override
    public String toString() {
        return "(" + getLeftOperand() + " += " + getRightOperand() + ")";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || !(obj instanceof StringConcatenateAssignmentNode)) {
            return false;
        }
        StringConcatenateAssignmentNode other = (StringConcatenateAssignmentNode) obj;
        return getLeftOperand().equals(other.getLeftOperand())
                && getRightOperand().equals(other.getRightOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeftOperand(), getRightOperand());
    }
}
