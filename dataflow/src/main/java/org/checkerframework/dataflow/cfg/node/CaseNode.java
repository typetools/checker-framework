package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.CaseTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.plumelib.util.StringsP;

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

  /**
   * The Node for the assignment of the switch selector expression to a synthetic local variable.
   */
  protected final AssignmentNode selectorExprAssignment;

  /**
   * The case expressions to match the switch expression against: the operands of (possibly
   * multiple) case labels.
   */
  protected final List<Node> caseExprs;

  /**
   * True if the CFG does not test the case expressions. This is so if they match every value of the
   * switch selector expression that is tested against them, or if this is the last case of an
   * exhaustive switch. False does not mean that the case expressions can fail to match; see {@link
   * #alwaysMatches()}.
   */
  protected final boolean alwaysMatches;

  /**
   * Creates a new CaseNode.
   *
   * @param tree the tree for this node
   * @param selectorExprAssignment the Node for the assignment of the switch selector expression to
   *     a synthetic local variable
   * @param caseExprs the case expression(s) to match the switch expression against
   * @param alwaysMatches true if the CFG does not test the case expressions; see {@link
   *     #alwaysMatches()}
   * @param types a factory of utility methods for operating on types
   */
  public CaseNode(
      CaseTree tree,
      AssignmentNode selectorExprAssignment,
      List<Node> caseExprs,
      boolean alwaysMatches,
      Types types) {
    super(types.getNoType(TypeKind.NONE));
    this.tree = tree;
    this.selectorExprAssignment = selectorExprAssignment;
    this.caseExprs = caseExprs;
    this.alwaysMatches = alwaysMatches;
  }

  /**
   * Returns the Node for the assignment of the switch selector expression to a synthetic local
   * variable. This is used to refine the type of the switch selector expression in a case block.
   *
   * @return the assignment of the switch selector expression to a synthetic local variable
   */
  public AssignmentNode getSwitchOperand() {
    return selectorExprAssignment;
  }

  /**
   * Returns the nodes corresponding to the case expressions. There can be multiple expressions
   * since Java 12.
   *
   * @return the nodes corresponding to the (potentially multiple) case expressions
   */
  public List<Node> getCaseOperands() {
    return caseExprs;
  }

  /**
   * Returns true if the CFG does not test the case expressions. This is so if they match every
   * value of the switch selector expression that is tested against them, or if this is the last
   * case of an exhaustive switch.
   *
   * <p>A false result means only that the CFG tests the case expressions, not that they can fail to
   * match. For example, given an enum with constants {@code A} and {@code B}, {@code case A, B}
   * matches every value of the selector expression, but if a {@code default} case follows it, then
   * the CFG tests it and this method returns false.
   *
   * @return true if the CFG does not test the case expressions
   */
  public boolean alwaysMatches() {
    return alwaysMatches;
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
    return "case " + StringsP.join(", ", getCaseOperands()) + ":";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof CaseNode other)) {
      return false;
    }
    return getSwitchOperand().equals(other.getSwitchOperand())
        && getCaseOperands().equals(other.getCaseOperands())
        && alwaysMatches == other.alwaysMatches;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSwitchOperand(), getCaseOperands(), alwaysMatches);
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    List<Node> caseOperands = getCaseOperands();
    ArrayList<Node> operands = new ArrayList<>(caseOperands.size() + 1);
    operands.add(getSwitchOperand());
    operands.addAll(caseOperands);
    return operands;
  }
}
