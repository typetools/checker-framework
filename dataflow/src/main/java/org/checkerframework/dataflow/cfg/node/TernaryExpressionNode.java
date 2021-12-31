package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a conditional expression:
 *
 * <pre>
 *   <em>expression</em> ? <em>expression</em> : <em>expression</em>
 * </pre>
 */
public class TernaryExpressionNode extends Node {

  protected final ConditionalExpressionTree tree;
  protected final Node condition;
  protected final Node thenOperand;
  protected final Node elseOperand;

  /**
   * This is a variable created by dataflow to which each case expression of the ternary expression
   * is assigned. Its value should be used for the value of the switch expression.
   */
  private final LocalVariableNode ternaryExpressionVar;

  public TernaryExpressionNode(
      ConditionalExpressionTree tree,
      Node condition,
      Node thenOperand,
      Node elseOperand,
      LocalVariableNode ternaryExpressionVar) {
    super(TreeUtils.typeOf(tree));
    assert tree.getKind() == Tree.Kind.CONDITIONAL_EXPRESSION;
    this.tree = tree;
    this.condition = condition;
    this.thenOperand = thenOperand;
    this.elseOperand = elseOperand;
    this.ternaryExpressionVar = ternaryExpressionVar;
  }

  public Node getConditionOperand() {
    return condition;
  }

  public Node getThenOperand() {
    return thenOperand;
  }

  public Node getElseOperand() {
    return elseOperand;
  }

  /**
   * This is a variable created by dataflow to which each case expression of the ternary expression
   * is assigned. Its value should be used for the value of the switch expression.
   *
   * @return the variable for this ternary expression
   */
  public LocalVariableNode getTernaryExpressionVar() {
    return ternaryExpressionVar;
  }

  @Override
  public ConditionalExpressionTree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitTernaryExpression(this, p);
  }

  @Override
  public String toString() {
    return "(" + getConditionOperand() + " ? " + getThenOperand() + " : " + getElseOperand() + ")";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof TernaryExpressionNode)) {
      return false;
    }
    TernaryExpressionNode other = (TernaryExpressionNode) obj;
    return getConditionOperand().equals(other.getConditionOperand())
        && getThenOperand().equals(other.getThenOperand())
        && getElseOperand().equals(other.getElseOperand());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getConditionOperand(), getThenOperand(), getElseOperand());
  }

  @Override
  public Collection<Node> getOperands() {
    return Arrays.asList(getConditionOperand(), getThenOperand(), getElseOperand());
  }
}
