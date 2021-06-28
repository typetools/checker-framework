package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;

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
  protected final Node caseExpr;

  /**
   * Create a new CaseNode.
   *
   * @param tree the tree for this node
   * @param switchExpr the switch expression
   * @param caseExpr the case expression to match the switch expression against
   * @param types a factory of utility methods for operating on types
   */
  public CaseNode(CaseTree tree, Node switchExpr, Node caseExpr, Types types) {
    super(types.getNoType(TypeKind.NONE));
    assert tree.getKind() == Tree.Kind.CASE;
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
  public boolean equals(@Nullable Object obj) {
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
    return Arrays.asList(getSwitchOperand(), getCaseOperand());
  }
}
