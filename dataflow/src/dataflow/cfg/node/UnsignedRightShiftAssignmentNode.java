package checkers.flow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for bitwise right shift compound assignments with zero extension:
 *
 * <pre>
 *   <em>expression</em> >>>= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class UnsignedRightShiftAssignmentNode extends CompoundAssignmentNode {

    public UnsignedRightShiftAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, right, right);
        assert tree.getKind() == Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitUnsignedRightShiftAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return ">>>";
    }
}
