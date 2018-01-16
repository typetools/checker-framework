package org.checkerframework.framework.util.typeinference8.constraint;

import org.checkerframework.framework.util.typeinference8.bound.BoundSet;

/**
 * A result of reduction. One of: a constraint, a set of constraints, a bound set, or a bound set
 * and a constraint.
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
        public ConstraintSet first;
        public BoundSet second;

        public static ReductionResultPair of(ConstraintSet first, BoundSet second) {
            ReductionResultPair pair = new ReductionResultPair();
            pair.first = first;
            pair.second = second;
            return pair;
        }
    }
}
