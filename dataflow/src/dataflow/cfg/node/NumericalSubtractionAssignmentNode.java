package dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node for the numerical subtraction compound assignment:
 *
 * <pre>
 *   <em>variable</em> -= <em>expression</em>
 * </pre>
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class NumericalSubtractionAssignmentNode extends CompoundAssignmentNode {

    public NumericalSubtractionAssignmentNode(Tree tree, Node left, Node right) {
        super(tree, left, right);
        assert tree.getKind() == Kind.MINUS_ASSIGNMENT;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitNumericalSubtractionAssignment(this, p);
    }

    @Override
    public String getOperator() {
        return "-";
    }
}
