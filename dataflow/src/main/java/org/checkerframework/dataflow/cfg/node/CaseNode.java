package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.util.StringsPlume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

/**
 * A node for a case in a switch statement. Although a case has no abstract value, it can imply
 * facts about the abstract values of its operands.
 *
 * <pre>
 *   case <em>constant</em>:
 * </pre>
 */
public class CaseNode extends Node {

    /** The tree for this node. */
    protected final CaseTree tree;
    /** The switch expression. */
    protected final Node switchExpr;
    /** The case expression to match the switch expression against. */
    protected final List<Node> caseExprs;

    /**
     * Create a new CaseNode.
     *
     * @param tree the tree for this node
     * @param switchExpr the switch expression
     * @param caseExprs the case expression(s) to match the switch expression against
     * @param types a factory of utility methods for operating on types
     */
    public CaseNode(CaseTree tree, Node switchExpr, List<Node> caseExprs, Types types) {
        super(types.getNoType(TypeKind.NONE));
        assert tree.getKind() == Tree.Kind.CASE;
        this.tree = tree;
        this.switchExpr = switchExpr;
        this.caseExprs = caseExprs;
    }

    public Node getSwitchOperand() {
        return switchExpr;
    }

    /**
     * Gets the nodes corresponding to the case expressions. There can be multiple expressions since
     * Java 12.
     *
     * @return the nodes corresponding to the (potentially multiple) case expressions
     */
    public List<Node> getCaseOperands() {
        return caseExprs;
    }

    @Override
    public CaseTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitCase(this, p);
    }

    @Override
    public String toString() {
        return "case " + StringsPlume.join(", ", getCaseOperands()) + ":";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CaseNode)) {
            return false;
        }
        CaseNode other = (CaseNode) obj;
        return getSwitchOperand().equals(other.getSwitchOperand())
                && getCaseOperands().equals(other.getCaseOperands());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSwitchOperand(), getCaseOperands());
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> operands = new ArrayList<>();
        operands.add(getSwitchOperand());
        operands.addAll(getCaseOperands());
        return operands;
    }
}
