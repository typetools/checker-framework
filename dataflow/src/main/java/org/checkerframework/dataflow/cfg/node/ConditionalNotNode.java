package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import java.util.Objects;

/**
 * A node for a conditional not expression:
 *
 * <pre>
 *   ! <em>expression</em>
 * </pre>
 */
public class ConditionalNotNode extends UnaryOperationNode {

    public ConditionalNotNode(UnaryTree tree, Node operand) {
        super(tree, operand);
        assert tree.getKind().equals(Kind.LOGICAL_COMPLEMENT);
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitConditionalNot(this, p);
    }

    @Override
    public String toString() {
        return "(!" + getOperand() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConditionalNotNode)) {
            return false;
        }
        ConditionalNotNode other = (ConditionalNotNode) obj;
        return getOperand().equals(other.getOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ConditionalNotNode.class, getOperand());
    }
}
