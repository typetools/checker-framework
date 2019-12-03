package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A node for the numerical multiplication:
 *
 * <pre>
 *   <em>expression</em> * <em>expression</em>
 * </pre>
 */
public class NumericalMultiplicationNode extends BinaryOperationNode {

    public NumericalMultiplicationNode(BinaryTree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.MULTIPLY;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitNumericalMultiplication(this, p);
    }

    @Override
    public String toString() {
        return "(" + getLeftOperand() + " * " + getRightOperand() + ")";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof NumericalMultiplicationNode)) {
            return false;
        }
        NumericalMultiplicationNode other = (NumericalMultiplicationNode) obj;
        return getLeftOperand().equals(other.getLeftOperand())
                && getRightOperand().equals(other.getRightOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeftOperand(), getRightOperand());
    }
}
