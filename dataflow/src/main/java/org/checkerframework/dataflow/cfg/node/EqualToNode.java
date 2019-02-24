package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;
import java.util.Objects;

/**
 * A node for an equality check:
 *
 * <pre>
 *   <em>expression</em> == <em>expression</em>
 * </pre>
 */
public class EqualToNode extends BinaryOperationNode {

    public EqualToNode(BinaryTree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind().equals(Kind.EQUAL_TO);
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitEqualTo(this, p);
    }

    @Override
    public String toString() {
        return "(" + getLeftOperand() + " == " + getRightOperand() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EqualToNode)) {
            return false;
        }
        EqualToNode other = (EqualToNode) obj;
        return getLeftOperand().equals(other.getLeftOperand())
                && getRightOperand().equals(other.getRightOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeftOperand(), getRightOperand());
    }
}
