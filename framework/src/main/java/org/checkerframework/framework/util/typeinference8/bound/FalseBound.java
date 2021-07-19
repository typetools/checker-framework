package org.checkerframework.framework.util.typeinference8.bound;

import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;

public class FalseBound implements ReductionResult {

  /** Was this false bound created because an annotated type constraint could not be satisfied? */
  private final boolean annotatedTypeFailure;

  /**
   * @param annotatedTypeFailure true if this false bound was caused by an annotated type constraint
   */
  public FalseBound(boolean annotatedTypeFailure) {
    this.annotatedTypeFailure = annotatedTypeFailure;
  }

  /**
   * Return true if this false bound was caused by an annotated type.
   *
   * @return true if this false bound was caused by annotated type.
   */
  public boolean isAnnotatedTypeFailure() {
    return annotatedTypeFailure;
  }

  @Override
  public String toString() {
    return "FalseBound{" + "annotatedTypeFailure=" + annotatedTypeFailure + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FalseBound that = (FalseBound) o;

    return annotatedTypeFailure == that.annotatedTypeFailure;
  }

  @Override
  public int hashCode() {
    return (annotatedTypeFailure ? 1 : 0);
  }
}
