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
   * The nodes for this case's labels, which the switch matches the selector expression against; see
   * {@link #getCaseOperands()}.
   */
  protected final List<Node> caseExprs;

  /** True if the CFG omits the test of this case's labels; see {@link #labelsUntested()}. */
  protected final boolean labelsUntested;

  /**
   * True if code that might have side effects can run between the evaluation of the switch selector
   * expression and this node; see {@link #sideEffectSinceSelector()}.
   */
  protected final boolean sideEffectSinceSelector;

  /**
   * Creates a new CaseNode.
   *
   * @param tree the tree for this node
   * @param selectorExprAssignment the Node for the assignment of the switch selector expression to
   *     a synthetic local variable
   * @param caseExprs the nodes for this case's labels, which the switch matches the selector
   *     expression against
   * @param labelsUntested true if the CFG omits the test of this case's labels; see {@link
   *     #labelsUntested()}
   * @param sideEffectSinceSelector true if code that might have side effects can run between the
   *     evaluation of the switch selector expression and this node; see {@link
   *     #sideEffectSinceSelector()}
   * @param types a factory of utility methods for operating on types
   */
  public CaseNode(
      CaseTree tree,
      AssignmentNode selectorExprAssignment,
      List<Node> caseExprs,
      boolean labelsUntested,
      boolean sideEffectSinceSelector,
      Types types) {
    super(types.getNoType(TypeKind.NONE));
    this.tree = tree;
    this.selectorExprAssignment = selectorExprAssignment;
    this.caseExprs = caseExprs;
    this.labelsUntested = labelsUntested;
    this.sideEffectSinceSelector = sideEffectSinceSelector;
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
   * Returns the nodes for this case's labels. A label is a constant or pattern after {@code case},
   * such as {@code RED} or {@code GREEN} in {@code case RED, GREEN ->}. Since Java 12, a case can
   * have multiple labels.
   *
   * @return the nodes for this case's labels
   */
  public List<Node> getCaseOperands() {
    return caseExprs;
  }

  /**
   * Returns true if the CFG omits the test of this case's labels (see {@link #getCaseOperands()}).
   * This concerns only the labels of this one case, not the labels of the switch's other cases. If
   * this is true, the CFG has no branch from this node to the next case: control always proceeds to
   * the guard or the case body, as if one of this case's labels matched.
   *
   * <p>This is true if this case's single label matches every value of the switch selector
   * expression that is tested against it.
   *
   * <p>This is also true for the last case of an exhaustive switch that has no default case. Java
   * requires a switch expression or an enhanced switch statement (JLS 14.11.2) to be exhaustive:
   * the labels of all its cases, together, must cover every value of the selector expression's
   * type. Control reaches the last case only if no earlier case matched, so the last case's labels
   * match every value that reaches it. An exception is an enum constant that was added after the
   * switch was compiled: it matches no label of any case, and the switch throws a MatchException,
   * which the CFG does not represent.
   *
   * <p>A false result means only that the CFG tests this case's labels, not that they can fail to
   * match. For example, given an enum with constants {@code A} and {@code B}, {@code case A, B}
   * matches every value of the selector expression, but if a {@code default} case follows it, then
   * the CFG tests it and this method returns false.
   *
   * @return true if the CFG omits the test of this case's labels
   */
  public boolean labelsUntested() {
    return labelsUntested;
  }

  /**
   * Returns true if code that might have side effects, such as a guard or a deconstruction pattern,
   * can run between the evaluation of the switch selector expression and this node. If so, the
   * selector expression (the right-hand side of {@link #getSwitchOperand()}) might no longer denote
   * the value that the switch tests, for example if it is a field that a guard reassigned. The
   * synthetic variable (the left-hand side of {@link #getSwitchOperand()}) always denotes that
   * value.
   *
   * @return true if code that might have side effects can run between the evaluation of the switch
   *     selector expression and this node
   */
  public boolean sideEffectSinceSelector() {
    return sideEffectSinceSelector;
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
    return "case "
        + StringsP.join(", ", getCaseOperands())
        + ":"
        + (labelsUntested ? " (labels untested)" : "");
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof CaseNode other)) {
      return false;
    }
    return getSwitchOperand().equals(other.getSwitchOperand())
        && getCaseOperands().equals(other.getCaseOperands())
        && labelsUntested == other.labelsUntested
        && sideEffectSinceSelector == other.sideEffectSinceSelector;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getSwitchOperand(), getCaseOperands(), labelsUntested, sideEffectSinceSelector);
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
