package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.SwitchExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** A node for a switch expression. */
public class SwitchExpressionNode extends Node {

  /** The {@code SwitchExpressionTree} corresponding to this node. */
  private final SwitchExpressionTree switchExpressionTree;

  /**
   * This is a variable created by dataflow to which each result expression of the switch expression
   * is assigned. Its value should be used for the value of the switch expression.
   */
  private final LocalVariableNode switchExpressionVar;

  /**
   * Creates a new SwitchExpressionNode.
   *
   * @param type the type of the node
   * @param switchExpressionTree the {@code SwitchExpressionTree} for this node
   * @param switchExpressionVar a variable created by dataflow to which each result expression of
   *     the switch expression is assigned. Its value should be used for the value of the switch
   *     expression
   */
  public SwitchExpressionNode(
      TypeMirror type,
      SwitchExpressionTree switchExpressionTree,
      LocalVariableNode switchExpressionVar) {
    super(type);

    this.switchExpressionTree = switchExpressionTree;
    this.switchExpressionVar = switchExpressionVar;
  }

  @Override
  public SwitchExpressionTree getTree() {
    return switchExpressionTree;
  }

  /**
   * This is a variable created by dataflow to which each result expression of the switch expression
   * is assigned. Its value should be used for the value of the switch expression.
   *
   * @return the variable for this switch expression
   */
  public LocalVariableNode getSwitchExpressionVar() {
    return switchExpressionVar;
  }

  @Override
  public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
    return visitor.visitSwitchExpressionNode(this, p);
  }

  @Override
  @SideEffectFree
  public Collection<Node> getOperands() {
    return Collections.singleton(switchExpressionVar);
  }

  @Override
  public String toString() {
    return "SwitchExpressionNode{"
        + "switchExpressionTree="
        + switchExpressionTree
        + ", switchExpressionVar="
        + switchExpressionVar
        + '}';
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof SwitchExpressionNode other)) {
      return false;
    }
    return getTree().equals(other.getTree())
        && getSwitchExpressionVar().equals(other.getSwitchExpressionVar());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTree(), getSwitchExpressionVar());
  }
}
