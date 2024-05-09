package org.checkerframework.dataflow.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.javacutil.BugInCF;

/**
 * The part of a <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.13">Java Method
 * Reference expression</a> that precedes "::". It can have the following forms:
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
   * ClassType}, or {@literal ArrayType}.
   */
  private final @Nullable JavaExpression type;

  /** True if this method reference scope is "super". */
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
    if (isReceiverSuper) {
      // If the scope is "super", the expression and type must both be null
      if (!(expression == null && type == null)) {
        throw new BugInCF("Malformed MethodReferenceScope");
      }
    }
    this.expression = expression;
    this.type = type;
    this.isSuper = isReceiverSuper;
  }

  /**
   * Return the expression for this method reference scope, or null if it's not an expression.
   *
   * @return the expression for this method reference scope
   */
  @Pure
  public @Nullable JavaExpression getExpression() {
    return this.expression;
  }

  /**
   * Return the type for this method reference scope, or null if it's not a type.
   *
   * @return the type for this method reference scope
   */
  @Pure
  public @Nullable JavaExpression getType() {
    return this.type;
  }

  /**
   * Return true if this method reference scope is "super".
   *
   * @return true if this method reference scope is "super"
   */
  public boolean isSuper() {
    return this.isSuper;
  }

  /**
   * Return the first subexpression in this method reference scope whose class is the given class,
   * or null.
   *
   * @param clazz the class
   * @return the first subexpression in this method reference scope whose class is the given class,
   *     or null
   * @param <T> the class
   */
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    if (isSuper()) {
      return null;
    }
    T result = null;
    if (getExpression() != null) {
      result = getExpression().containedOfClass(clazz);
      if (result != null) {
        return result;
      }
    }
    if (getType() != null) {
      result = getType().containedOfClass(clazz);
    }
    return result;
  }

  @Override
  public String toString() {
    if (isSuper()) {
      return "super";
    } else if (expression != null) {
      return expression.toString();
    } else if (type != null) {
      return type.toString();
    } else {
      throw new BugInCF("Malformed MethodReferenceScope");
    }
  }
}
