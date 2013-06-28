package dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for bitwise left shift compound assignments:
 *
 * <pre>
 *   <em>variable</em> <<= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class LeftShiftAssignmentNode extends CompoundAssignmentNode {

    public LeftShiftAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.LEFT_SHIFT_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitLeftShiftAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return "<<";
    }
}
