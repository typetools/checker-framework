package org.checkerframework.framework.util.typeinference8.constraint;

import org.checkerframework.framework.util.typeinference8.bound.BoundSet;

/**
 * A result of reduction. One of: {@link Constraint},{@link ConstraintSet},{@link BoundSet}, or
 * {@link ReductionResultPair}.
 */
public interface ReductionResult {

  /**
   * Indicates that the constraint reduced to true, but unchecked conversion is required for the
   * method to be applicable.
   */
  ReductionResult UNCHECKED_CONVERSION =
      new ReductionResult() {
        @Override
        public String toString() {
          return "UNCHECKED_CONVERSION";
        }
      };

  /** A reduction result that contains a bound set and a constraint set. */
  class ReductionResultPair implements ReductionResult {
    public final ConstraintSet constraintSet;
    public final BoundSet boundSet;

    private ReductionResultPair(ConstraintSet constraintSet, BoundSet boundSet) {
      this.constraintSet = constraintSet;
      this.boundSet = boundSet;
    }

    public static ReductionResultPair of(ConstraintSet constraintSet, BoundSet boundSet) {
      ReductionResultPair pair = new ReductionResultPair(constraintSet, boundSet);
      return pair;
    }
  }
}
