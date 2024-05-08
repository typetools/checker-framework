package org.checkerframework.dataflow.expression;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The left-hand side of a <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.13">Java Method
 * Reference expression</a>
 *
 * <p>The left-hand side of a Java method Reference expression can have the following forms:
 *
 * <ul>
 *   <li>{@literal ExpressionName}
 *   <li>{@literal ReferenceType}
 *   <li>{@literal Primary}
 *   <li>{@literal "super"}
 *   <li>{@literal ClassType}
 *   <li>{@literal ArrayType}
 * </ul>
 */
public class MethodReferenceScope {

  /**
   * Non-null if this method reference scope is one of {@literal ExpressionName} or {@literal
   * Primary}.
   */
  private final @Nullable JavaExpression expression;

  /**
   * Non-null if this method reference scope is one of {@literal ReferenceType}, {@literal
   * ClassType}, {@literal ArrayType}.
   */
  private final @Nullable JavaExpression type;

  /** Whether this method reference scope is "super". */
  private final boolean isSuper;

  /**
   * Creates a new method reference scope.
   *
   * @param expression the expression
   * @param type the type
   * @param isReceiverSuper whether a method reference scope is "super"
   */
  public MethodReferenceScope(
      @Nullable JavaExpression expression, @Nullable JavaExpression type, boolean isReceiverSuper) {
    this.expression = expression;
    this.type = type;
    this.isSuper = isReceiverSuper;
  }

  /**
   * Return the expression for this method reference scope.
   *
   * @return the expression for this method reference scope
   */
  public @Nullable JavaExpression getExpression() {
    return this.expression;
  }

  /**
   * Return the type for this method reference scope.
   *
   * @return the type for this method reference scope
   */
  public @Nullable JavaExpression getType() {
    return this.type;
  }

  /**
   * Return true if this method reference scope is "super".
   *
   * @return true if this method reference scope is "super"
   */
  public boolean isScopeSuper() {
    return this.isSuper;
  }

  @Override
  public String toString() {
    if (isScopeSuper()) {
      return "super";
    }
    if (expression != null) {
      return expression.toString();
    }
    // One of expression or type has to be non-null
    assert type != null;
    return type.toString();
  }
}
