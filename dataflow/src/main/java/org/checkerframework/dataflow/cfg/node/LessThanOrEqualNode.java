package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Objects;

/**
 * A node for the less than or equal comparison:
 *
 * <pre>
 *   <em>expression</em> &lt;= <em>expression</em>
 * </pre>
 */
public class LessThanOrEqualNode extends BinaryOperationNode {

    public LessThanOrEqualNode(BinaryTree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.LESS_THAN_EQUAL;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitLessThanOrEqual(this, p);
    }

    @Override
    public String toString() {
        return "(" + getLeftOperand() + " <= " + getRightOperand() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LessThanOrEqualNode)) {
            return false;
        }
        LessThanOrEqualNode other = (LessThanOrEqualNode) obj;
        return getLeftOperand().equals(other.getLeftOperand())
                && getRightOperand().equals(other.getRightOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeftOperand(), getRightOperand());
    }
}
