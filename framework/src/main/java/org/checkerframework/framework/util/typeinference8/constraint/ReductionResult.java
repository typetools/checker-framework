package org.checkerframework.framework.util.typeinference8.constraint;

import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;

/**
 * A result of reduction. One of: {@link TypeConstraint},{@link ConstraintSet},{@link BoundSet}, or
 * {@link ReductionResultPair}.
 */
public interface ReductionResult {

  /**
   * Indicates that the constraint reduced to true, but unchecked conversion is required for the
   * method to be applicable.
   */
  @SuppressWarnings("interning:assignment")
  @InternedDistinct ReductionResult UNCHECKED_CONVERSION =
      new ReductionResult() {
        @Override
        public String toString() {
          return "UNCHECKED_CONVERSION";
        }
      };

  /** A reduction result that contains a bound set and a constraint set. */
  class ReductionResultPair implements ReductionResult {

    /** A constraint set. */
    public final ConstraintSet constraintSet;

    /** A bound set. */
    public final BoundSet boundSet;

    /**
     * Creates a reduction result pair.
     *
     * @param constraintSet a constraint set
     * @param boundSet a bound set
     */
    private ReductionResultPair(ConstraintSet constraintSet, BoundSet boundSet) {
      this.constraintSet = constraintSet;
      this.boundSet = boundSet;
    }

    /**
     * Creates a reduction result pair.
     *
     * @param constraintSet a constraint set
     * @param boundSet a bound set
     * @return a reduction result pair
     */
    public static ReductionResultPair of(ConstraintSet constraintSet, BoundSet boundSet) {
      ReductionResultPair pair = new ReductionResultPair(constraintSet, boundSet);
      return pair;
    }
  }
}
