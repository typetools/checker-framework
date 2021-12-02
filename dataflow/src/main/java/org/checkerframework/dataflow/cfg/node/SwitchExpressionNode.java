package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.StringsPlume;

/**
 * A node for a switch expression:
 *
 * <pre>
 *   switch (<em>expression</em>) {
 *     cases
 *   }
 * </pre>
 */
public class SwitchExpressionNode extends Node {

  /**
   * The SwitchExpressionTree. (The type must be expression tree so that this code compiles on Java
   * 8 and 11.)
   */
  protected final ExpressionTree tree;

  protected final Node expression;
  protected final List<Node> cases;

  public SwitchExpressionNode(ExpressionTree tree, Node expression, List<Node> cases) {
    super(TreeUtils.typeOf(tree));
    assert tree.getKind().name().equals("SWITCH_EXPRESSION");
    this.tree = tree;
    this.expression = expression;
    this.cases = cases;
  }

  public Node getExpression() {
    return expression;
  }

  public List<Node> getCases() {
    return cases;
  }

  @Override
  public ExpressionTree getTree() {
    return tree;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitSwitchExpression(this, p);
  }

  @Override
  public String toString() {
    return "switch (" + getExpression() + ") { " + StringsPlume.join("; ", getCases()) + " }";
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof SwitchExpressionNode)) {
      return false;
    }
    SwitchExpressionNode other = (SwitchExpressionNode) obj;
    return getExpression().equals(other.getExpression()) && getCases().equals(other.getCases());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getExpression(), getCases());
  }

  @Override
  public Collection<Node> getOperands() {
    return Collections.singleton(getExpression());
  }
}
