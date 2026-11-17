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
   * exhaustive switch. The last case of an exhaustive switch does not necessarily match every
   * value: for example, an enum constant that was added after the switch was compiled matches no
   * case. Such a value causes the switch to throw an exception, which is an exceptional successor
   * of this node. False does not mean that the case expressions can fail to match; see {@link
   * #labelsUntested()}.
   */
  protected final boolean labelsUntested;

  /**
   * True if code that might have side effects, such as the guard of an earlier case or a
   * deconstruction pattern, can run between the evaluation of the switch selector expression and
   * this node. If so, the selector expression might no longer denote the value that the switch
   * tests. See {@link #codeRanSinceSelector()}.
   */
  protected final boolean codeRanSinceSelector;

  /**
   * True if testing this case's labels might have a side effect: that is, if a record accessor that
   * a record pattern among the labels invokes, directly or through a nested record pattern, is not
   * side-effect-free. See {@link #labelsMightHaveSideEffects()}.
   */
  protected final boolean labelsMightHaveSideEffects;

  /**
   * Creates a new CaseNode.
   *
   * @param tree the tree for this node
   * @param selectorExprAssignment the Node for the assignment of the switch selector expression to
   *     a synthetic local variable
   * @param caseExprs the case expression(s) to match the switch expression against
   * @param labelsUntested true if the CFG does not test the case expressions; see {@link
   *     #labelsUntested()}
   * @param codeRanSinceSelector true if code that might have side effects can run between the
   *     evaluation of the switch selector expression and this node; see {@link
   *     #codeRanSinceSelector()}
   * @param labelsMightHaveSideEffects true if testing the case expressions might have a side
   *     effect; see {@link #labelsMightHaveSideEffects()}
   * @param types a factory of utility methods for operating on types
   */
  public CaseNode(
      CaseTree tree,
      AssignmentNode selectorExprAssignment,
      List<Node> caseExprs,
      boolean labelsUntested,
      boolean codeRanSinceSelector,
      boolean labelsMightHaveSideEffects,
      Types types) {
    super(types.getNoType(TypeKind.NONE));
    this.tree = tree;
    this.selectorExprAssignment = selectorExprAssignment;
    this.caseExprs = caseExprs;
    this.labelsUntested = labelsUntested;
    this.codeRanSinceSelector = codeRanSinceSelector;
    this.labelsMightHaveSideEffects = labelsMightHaveSideEffects;
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
   * case of an exhaustive switch. The last case of an exhaustive switch does not necessarily match
   * every value: for example, an enum constant that was added after the switch was compiled matches
   * no case. Such a value causes the switch to throw an exception, which is an exceptional
   * successor of this node.
   *
   * <p>A false result means only that the CFG tests the case expressions, not that they can fail to
   * match. For example, given an enum with constants {@code A} and {@code B}, {@code case A, B}
   * matches every value of the selector expression, but if a {@code default} case follows it, then
   * the CFG tests it and this method returns false.
   *
   * @return true if the CFG does not test the case expressions
   */
  public boolean labelsUntested() {
    return labelsUntested;
  }

  /**
   * Returns true if code that might have side effects can run between the evaluation of the switch
   * selector expression and this node, including while this case's labels are tested. Such code is
   * the guard of an earlier case, or a record accessor that a deconstruction pattern of this case
   * or an earlier case invokes. (The CFG does not represent the record accessor calls.)
   *
   * <p>If so, that code might have changed the value of the switch selector expression (the
   * right-hand side of {@link #getSwitchOperand()}), which then might no longer denote the value
   * that the switch tests. For example, the code might assign a field that is the selector
   * expression, or might mutate the object that the selector expression evaluates to. Then, a fact
   * about the value that was tested against the case expressions need not hold for the selector
   * expression. It does hold for the synthetic variable (the left-hand side of {@link
   * #getSwitchOperand()}), which always denotes that value.
   *
   * @return true if code that might have side effects can run between the evaluation of the switch
   *     selector expression and this node
   */
  public boolean codeRanSinceSelector() {
    return codeRanSinceSelector;
  }

  /**
   * Returns true if testing this case's labels might have a side effect: that is, if a record
   * accessor that a record pattern among the labels invokes, directly or through a nested record
   * pattern, is not side-effect-free. The record pattern invokes the accessors even if the match
   * fails. The CFG does not represent the accessor calls, so a transfer function must account for
   * their effect at this node. If this method returns true, so does {@link
   * #codeRanSinceSelector()}.
   *
   * @return true if testing this case's labels might have a side effect
   */
  public boolean labelsMightHaveSideEffects() {
    return labelsMightHaveSideEffects;
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
        && codeRanSinceSelector == other.codeRanSinceSelector
        && labelsMightHaveSideEffects == other.labelsMightHaveSideEffects;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getSwitchOperand(),
        getCaseOperands(),
        labelsUntested,
        codeRanSinceSelector,
        labelsMightHaveSideEffects);
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
