package dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for the bitwise or logical (single bit) xor compound assignment:
 *
 * <pre>
 *   <em>variable</em> ^= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class BitwiseXorAssignmentNode extends CompoundAssignmentNode {

    public BitwiseXorAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.XOR_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitBitwiseXorAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return "^";
    }
}
