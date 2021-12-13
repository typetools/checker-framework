package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;

/** A node for a switch expression. */
public class SwitchExpressionNode extends Node {

  /** The {@code SwitchExpressionTree} corresponding to this node. */
  private final Tree switchExpressionTree;

  /**
   * This is a variable created by dataflow to which each result expression of the switch expression
   * is assigned. Its value should be used for the value of the switch expression.
   */
  private final LocalVariableNode switchExpressionVar;

  /**
   * Creates a new SwitchExpressionNoode.
   *
   * @param type the type of the node
   * @param switchExpressionTree the {@code SwitchExpressionTree} for this node
   * @param switchExpressionVar a variable created by dataflow to which each result expression of
   *     the switch expression is assigned. Its value should be used for the value of the switch
   *     expression
   */
  public SwitchExpressionNode(
      TypeMirror type, Tree switchExpressionTree, LocalVariableNode switchExpressionVar) {
    super(type);

    if (switchExpressionTree.getKind().name().equals("SWITCH_EXPRESSION")) {
      throw new BugInCF(
          "switchExpressionTree is not a SwitchExpressionTree found tree with kind %s instead.",
          switchExpressionTree.getKind());
    }

    this.switchExpressionTree = switchExpressionTree;
    this.switchExpressionVar = switchExpressionVar;
  }

  @Override
  public Tree getTree() {
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
    if (!(obj instanceof SwitchExpressionNode)) {
      return false;
    }
    SwitchExpressionNode other = (SwitchExpressionNode) obj;
    return getTree().equals(other.getTree())
        && getSwitchExpressionVar().equals(other.getSwitchExpressionVar());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTree(), getSwitchExpressionVar());
  }
}
