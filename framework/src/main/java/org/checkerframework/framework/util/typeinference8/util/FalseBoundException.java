package org.checkerframework.framework.util.typeinference8.util;

import org.checkerframework.framework.util.typeinference8.bound.FalseBound;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;

public class FalseBoundException extends RuntimeException {
  private static final long serialVersionUID = 1;
  private final boolean annotatedTypeFailed;

  public FalseBoundException(Constraint constraint, ReductionResult result) {
    super("Constraint: " + constraint);
    if (result instanceof FalseBound) {
      annotatedTypeFailed = ((FalseBound) result).isAnnotatedTypeFailure();
    } else {
      annotatedTypeFailed = false;
    }
  }

  public boolean isAnnotatedTypeFailed() {
    return annotatedTypeFailed;
  }
}
