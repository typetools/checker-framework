package org.checkerframework.checker.index;

import org.checkerframework.checker.index.substringindex.SubstringIndexAnnotatedTypeFactory;
import org.checkerframework.checker.index.upperbound.UpperBoundAnnotatedTypeFactory;
import org.checkerframework.checker.index.upperbound.UpperBoundChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * A type checker for preventing out-of-bounds accesses on fixed-length sequences, such as arrays
 * and strings. Contains five subcheckers that do all of the actual work, which are described here.
 * First, the order the checkers are run in is described, and then what each checker requests from
 * the checkers that run before it is described. The Index Checker itself is just an alias for the
 * Upper Bound Checker, which runs last.
 *
 * <p>The checkers run in this order:
 *
 * <p>Constant Value Checker, SameLen Checker, SearchIndex Checker, Lower Bound Checker, Upper Bound
 * Checker
 *
 * <p>The Constant Value Checker has no dependencies, but it does trust Positive annotations from
 * the Lower Bound Checker. This means that if the Value Checker is run on code containing Positive
 * annotations, then the Lower Bound Checker also needs to be run to guarantee soundness.
 *
 * <p>The SameLen Checker has no dependencies.
 *
 * <p>The SearchIndex Checker depends only on the Value Checker, which it uses to refine
 * SearchIndexFor types to NegativeIndexFor types by comparing to compile-time constants of zero or
 * negative one.
 *
 * <p>The Lower Bound Checker depends only on the Value Checker. It uses the Value Checker to:
 *
 * <ul>
 *   <li>give appropriate types to compile time constants. For example, the type of 7 is Positive, 0
 *       is NonNegative, etc.
 *   <li>in a subtraction expression of the form {@code a.length - x}, if x is a compile-time
 *       constant, and if the minimum length of a &gt; x, the resulting expression is non-negative.
 *   <li>when typing an array length (i.e. {@code a.length}), if the minimum length of the array is
 *       &ge; 1, then the type is @Positive; if its MinLen is zero, then the type is @NonNegative.
 * </ul>
 *
 * <p>The Upper Bound Checker depends on all three other checkers.
 *
 * <p>Value dependencies in the UBC:
 *
 * <ul>
 *   <li>When computing offsets, the UBC replaces compile-time constants with their known values
 *       (though it also keeps an offset with the variable's name, if applicable).
 *   <li>The UBC has relaxed assignment rules: it allows assignments where the right hand side is a
 *       value known at compile time and the type of the left hand side is annotated with
 *       LT*LengthOf("a"). If the minimum length of a is in the correct relationship with the value
 *       on the right hand side, then the assignment is legal.
 *   <li>When checking whether an array access is legal, the UBC first checks the upper bound type
 *       of the index. If that fails, it checks if the index is a compile-time constant. If it is,
 *       then it queries the Value Checker to determine if the array is guaranteed to be longer than
 *       the value of the constant. If it is, the access is safe.
 *   <li>When compile time constants would improve the precision of reasoning about arithmetic, the
 *       UBC queries the Value Checker for their values. For instance, dividing a value with type
 *       LTLengthOf by a compile-time constant of 1 is guaranteed to result in another LTLengthOf
 *       for the same arrays.
 * </ul>
 *
 * <p>SameLen dependencies in the UBC:
 *
 * <ul>
 *   <li>When checking whether an array access is legal, the UBC first checks the upper bound type.
 *       If it's an LTL (or LTOM/LTEL), then it collects, from the SameLen Checker, the list of
 *       arrays that are known to be the same length as the array being accessed. Then the
 *       annotation is checked to see if it is valid for any of the arrays in question.
 * </ul>
 *
 * <p>Lower Bound dependencies in UBC:
 *
 * <ul>
 *   <li>When an array is created with length equal to the sum of two quantities, if one of the
 *       quantities is non-negative, the other becomes LTEL of the new array. If one is positive,
 *       the other becomes LTL.
 *   <li>When a non-negative is subtracted from an LTL, it stays LTL.
 * </ul>
 *
 * @checker_framework.manual #index-checker Index Checker
 */
// @RelevantJavaTypes annotations appear on other checkers.
public class IndexChecker extends UpperBoundChecker {

    /** The SubstringIndexAnnotatedTypeFactory associated with this. */
    private @MonotonicNonNull SubstringIndexAnnotatedTypeFactory substringIndexAtypeFactory;

    /** The UpperBoundAnnotatedTypeFactory associated with this. */
    private @MonotonicNonNull UpperBoundAnnotatedTypeFactory upperBoundAtypeFactory;

    /** Creates the Index Chceker. */
    public IndexChecker() {}

    /**
     * Sets the SubstringIndexAnnotatedTypeFactory associated with this.
     *
     * @param substringIndexAtypeFactory the SubstringIndexAnnotatedTypeFactory associated with this
     */
    public void setSubstringIndexAtypeFactory(
            SubstringIndexAnnotatedTypeFactory substringIndexAtypeFactory) {
        this.substringIndexAtypeFactory = substringIndexAtypeFactory;
        introduceSubcheckers();
    }

    /**
     * Sets the UpperBoundAnnotatedTypeFactory associated with this.
     *
     * @param upperBoundAtypeFactory the UpperBoundAnnotatedTypeFactory associated with this
     */
    public void setUpperBoundAtypeFactory(UpperBoundAnnotatedTypeFactory upperBoundAtypeFactory) {
        this.upperBoundAtypeFactory = upperBoundAtypeFactory;
        introduceSubcheckers();
    }

    /**
     * Introduce the subcheckers to one another: set fields that link between them. Calling this has
     * no effect until all the needed fields have been set.
     */
    private void introduceSubcheckers() {
        if (upperBoundAtypeFactory != null && substringIndexAtypeFactory != null) {
            substringIndexAtypeFactory.upperBoundAtypeFactory = upperBoundAtypeFactory;
            upperBoundAtypeFactory.substringIndexAtypeFactory = substringIndexAtypeFactory;
        }
    }
}
