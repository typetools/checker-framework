package org.checkerframework.dataflow.busyexpr;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;

/**
 * The BusyExprValue class contains a BinaryOperationNode, so we only consider expressions that are
 * in the form of a BinaryOperationNode: <em>leftOperandNode</em> <em>operator</em>
 * <em>rightOperandNode</em>. We override {@code .equals} in this class to compare nodes by value
 * equality rather than reference equality. We want two different nodes with the same value (that
 * is, two nodes that refer to the same busy expression in the program) to be regarded as the same.
 */
public class BusyExprValue {

  /**
   * A busy expression is represented by a node, which can be a {@link
   * org.checkerframework.dataflow.cfg.node.BinaryOperationNode}.
   */
  protected final BinaryOperationNode busyExpression;

  /**
   * Creates a new busy expression.
   *
   * @param n a node
   */
  public BusyExprValue(BinaryOperationNode n) {
    this.busyExpression = n;
  }

  @Override
  public String toString() {
    return this.busyExpression.toString();
  }

  @Override
  public int hashCode() {
    return this.busyExpression.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof BusyExprValue other)) {
      return false;
    }
    // Use `equals` to check equality rather than using `==`.
    return this.busyExpression.equals(other.busyExpression);
  }
}
