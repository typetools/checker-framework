package org.checkerframework.checker.calledmethods;

import java.util.Objects;

/**
 * A postcondition contract that a method calls the given method on the given expression when that
 * method throws an exception.
 *
 * <p>Instances of this class are plain old immutable data with no interesting behavior.
 *
 * @see org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException
 */
// TODO: In the future, this class should be a record.
public class EnsuresCalledMethodOnExceptionContract {

  /** The expression described by this postcondition. */
  private final String expression;

  /** The method this postcondition promises to call. */
  private final String method;

  /**
   * Create a new {@code EnsuredCalledMethodOnException}. Usually this should be constructed from a
   * {@link org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException}
   * appearing in the source code.
   *
   * @param expression the expression described by this postcondition
   * @param method the method this postcondition promises to call
   */
  public EnsuresCalledMethodOnExceptionContract(String expression, String method) {
    this.expression = expression;
    this.method = method;
  }

  /**
   * The expression described by this postcondition.
   *
   * @return the expression described by this postcondition
   */
  public String getExpression() {
    return expression;
  }

  /**
   * The method this postcondition promises to call.
   *
   * @return the method this postcondition promises to call
   */
  public String getMethod() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EnsuresCalledMethodOnExceptionContract)) {
      return false;
    }
    EnsuresCalledMethodOnExceptionContract that = (EnsuresCalledMethodOnExceptionContract) o;
    return expression.equals(that.expression) && method.equals(that.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expression, method);
  }
}
