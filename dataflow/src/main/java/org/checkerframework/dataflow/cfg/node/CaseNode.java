package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Collection;
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

    protected final CaseTree tree;
    protected final Node switchExpr;
    protected final Node caseExpr;

    public CaseNode(CaseTree tree, Node switchExpr, Node caseExpr, Types types) {
        super(types.getNoType(TypeKind.NONE));
        assert tree.getKind().equals(Kind.CASE);
        this.tree = tree;
        this.switchExpr = switchExpr;
        this.caseExpr = caseExpr;
    }

    public Node getSwitchOperand() {
        return switchExpr;
    }

    public Node getCaseOperand() {
        return caseExpr;
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
        return "case " + getCaseOperand() + ":";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CaseNode)) {
            return false;
        }
        CaseNode other = (CaseNode) obj;
        return getSwitchOperand().equals(other.getSwitchOperand())
                && getCaseOperand().equals(other.getCaseOperand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSwitchOperand(), getCaseOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(2);
        list.add(getSwitchOperand());
        list.add(getCaseOperand());
        return list;
    }
}
