package org.checkerframework.checker.calledmethods;

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

/**
 * A postcondition contract that a method calls the given method on the given expression when that
 * method throws an exception.
 *
 * <p>Instances of this class are plain old immutable data with no interesting behavior.
 *
 * @param expression The expression described by this postcondition.
 * @param method The method this postcondition promises to call.
 * @see EnsuresCalledMethodsOnException
 */
public record EnsuresCalledMethodOnExceptionContract(String expression, String method) {

  /**
   * Create a new {@code EnsuredCalledMethodOnException}. Usually this should be constructed from a
   * {@link EnsuresCalledMethodsOnException} appearing in the source code.
   *
   * @param expression the expression described by this postcondition
   * @param method the method this postcondition promises to call
   */
  public EnsuresCalledMethodOnExceptionContract {}

  /**
   * The expression described by this postcondition.
   *
   * @return the expression described by this postcondition
   */
  @Override
  public String expression() {
    return expression;
  }

  /**
   * The method this postcondition promises to call.
   *
   * @return the method this postcondition promises to call
   */
  @Override
  public String method() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EnsuresCalledMethodOnExceptionContract that)) {
      return false;
    }
    return expression.equals(that.expression) && method.equals(that.method);
  }
}
