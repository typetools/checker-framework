package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Objects;

/**
 * A node for the not equal comparison:
 *
 * <pre>
 *   <em>expression</em> != <em>expression</em>
 * </pre>
 */
public class NotEqualNode extends BinaryOperationNode {

    public NotEqualNode(BinaryTree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.NOT_EQUAL_TO;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitNotEqual(this, p);
    }

    @Override
    public String toString() {
        return "(" + getLeftOperand() + " != " + getRightOperand() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NotEqualNode)) {
            return false;
        }
        NotEqualNode other = (NotEqualNode) obj;
        return getLeftOperand().equals(other.getLeftOperand())
                && getRightOperand().equals(other.getRightOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeftOperand(), getRightOperand());
    }
}
