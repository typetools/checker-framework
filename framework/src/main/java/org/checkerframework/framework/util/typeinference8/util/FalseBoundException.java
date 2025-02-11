package org.checkerframework.framework.util.typeinference8.util;

import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;

/** Exception thrown when the Java types make it so that false is inferred. */
public class FalseBoundException extends RuntimeException {

  /** serialVersionUID */
  private static final long serialVersionUID = 1;

  /**
   * Creates a false bound exception
   *
   * @param constraint the constraint the was not resolved
   * @param result the result of reduction
   */
  public FalseBoundException(Constraint constraint, ReductionResult result) {
    super(" False bound for: Constraint: " + constraint + " Result: " + result);
  }
}
