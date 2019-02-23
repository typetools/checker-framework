package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import java.util.Objects;

/**
 * A node for the unary plus operation:
 *
 * <pre>
 *   + <em>expression</em>
 * </pre>
 */
public class NumericalPlusNode extends UnaryOperationNode {

    public NumericalPlusNode(UnaryTree tree, Node operand) {
        super(tree, operand);
        assert tree.getKind() == Kind.UNARY_PLUS;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitNumericalPlus(this, p);
    }

    @Override
    public String toString() {
        return "(+ " + getOperand() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NumericalPlusNode)) {
            return false;
        }
        NumericalPlusNode other = (NumericalPlusNode) obj;
        return getOperand().equals(other.getOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOperand());
    }
}
