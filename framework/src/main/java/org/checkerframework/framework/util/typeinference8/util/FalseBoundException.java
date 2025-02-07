package org.checkerframework.framework.util.typeinference8.util;

import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.constraint.TypeConstraint;

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
  void constraintHistory(Constraint constraint){
    if(constraint instanceof TypeConstraint){
      StringBuilder constraintStack = new StringBuilder("\n");
      Constraint parent = ((TypeConstraint) constraint).parent;
      while(parent != null){
        constraintStack.append(parent);
        if(parent instanceof TypeConstraint){
          parent = ((TypeConstraint) constraint).parent;
        }
      }
    }
  }
}
