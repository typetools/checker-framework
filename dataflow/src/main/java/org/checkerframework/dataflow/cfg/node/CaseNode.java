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
   * True if code that might have side effects, such as the guard of an earlier case, might execute
   * between the evaluation of the switch selector expression and this case. If true, the current
   * value of the selector expression might differ from the value that this case tests.
   */
  protected final boolean selectorExprMayHaveChanged;

  /**
   * Creates a new CaseNode for a case such that no code executes between the evaluation of the
   * switch selector expression and the case.
   *
   * @param tree the tree for this node
   * @param selectorExprAssignment the Node for the assignment of the switch selector expression to
   *     a synthetic local variable
   * @param caseExprs the case expression(s) to match the switch expression against
   * @param types a factory of utility methods for operating on types
   */
  public CaseNode(
      CaseTree tree, AssignmentNode selectorExprAssignment, List<Node> caseExprs, Types types) {
    this(tree, selectorExprAssignment, caseExprs, false, types);
  }

  /**
   * Creates a new CaseNode.
   *
   * @param tree the tree for this node
   * @param selectorExprAssignment the Node for the assignment of the switch selector expression to
   *     a synthetic local variable
   * @param caseExprs the case expression(s) to match the switch expression against
   * @param selectorExprMayHaveChanged true if code that might have side effects, such as the guard
   *     of an earlier case, might execute between the evaluation of the switch selector expression
   *     and this case
   * @param types a factory of utility methods for operating on types
   */
  public CaseNode(
      CaseTree tree,
      AssignmentNode selectorExprAssignment,
      List<Node> caseExprs,
      boolean selectorExprMayHaveChanged,
      Types types) {
    super(types.getNoType(TypeKind.NONE));
    this.tree = tree;
    this.selectorExprAssignment = selectorExprAssignment;
    this.caseExprs = caseExprs;
    this.selectorExprMayHaveChanged = selectorExprMayHaveChanged;
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
   * Returns true if code that might have side effects, such as the guard of an earlier case, might
   * execute between the evaluation of the switch selector expression and this case. If so, the
   * current value of the selector expression might differ from the value that this case tests,
   * which is the value of the synthetic variable that is the target of {@link #getSwitchOperand()}.
   * For example, a guard might set a field that is the selector expression to null.
   *
   * @return true if the selector expression might have changed since it was evaluated
   */
  public boolean selectorExprMayHaveChanged() {
    return selectorExprMayHaveChanged;
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
        && getCaseOperands().equals(other.getCaseOperands());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSwitchOperand(), getCaseOperands());
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
