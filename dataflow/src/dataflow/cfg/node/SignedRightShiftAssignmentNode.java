package dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for bitwise right shift compound assignments with sign extension:
 *
 * <pre>
 *   <em>variable</em> >>= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class SignedRightShiftAssignmentNode extends CompoundAssignmentNode {

    public SignedRightShiftAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.RIGHT_SHIFT_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitSignedRightShiftAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return ">>";
    }
}
