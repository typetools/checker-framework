package dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for the integer remainder compound assignment:
 *
 * <pre>
 *   <em>variable</em> %= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class IntegerRemainderAssignmentNode extends CompoundAssignmentNode {

    public IntegerRemainderAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.REMAINDER_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitIntegerRemainderAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return "%";
    }
}
