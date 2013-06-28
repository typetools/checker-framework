package dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for the bitwise or logical (single bit) or compound assignment:
 *
 * <pre>
 *   <em>variable</em> |= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class BitwiseOrAssignmentNode extends CompoundAssignmentNode {

    public BitwiseOrAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.OR_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitBitwiseOrAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return "|";
    }
}
