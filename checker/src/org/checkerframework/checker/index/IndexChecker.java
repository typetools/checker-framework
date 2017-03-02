package org.checkerframework.checker.index;

import org.checkerframework.checker.index.upperbound.UpperBoundChecker;

/**
 * A type checker for preventing out-of-bounds accesses on arrays. Contains four subcheckers that do
 * all of the actual work, which are described here. First, the order the checkers are run in is
 * described, and then what each checker requests from the checkers that run before it is described.
 * The Index Checker itself is just an alias for the Upper Bound Checker, which runs last.
 *
 * <p>The checkers run in this order:
 *
 * <p>SameLen Checker, MinLen Checker, Lower Bound Checker, Upper Bound Checker
 *
 * <p>The SameLen Checker has no dependencies.
 *
 * <p>The MinLen Checker has no dependencies on the SameLen checker, so they may actually be run in
 * any order.
 *
 * <p>The Lower Bound Checker depends only on the MinLen Checker. It uses the MinLen checker to:
 *
 * <ul>
 *   <li> in a subtraction expression of the form `a.length - x`, if x is a compile time constant,
 *       the the LBC queries the MLC for the min length of a. If MinLen(a) &gt; x, the resulting
 *       expression is non-negative.
 *   <li> when typing an array length (i.e. `a.length`), if the MinLen is &gt;= 1, then the type
 *       is @Positive; if its MinLen is zero, then the type is @NonNegative.
 * </ul>
 *
 * <p>The Upper Bound Checker depends on all three other checkers.
 *
 * <p>MinLen dependencies in the UBC:
 *
 * <ul>
 *   <li> The UBC has relaxed assignment rules: it allows assignments where the right hand side is a
 *       value known at compile time and the type of the left hand side is annotated with
 *       LT*LengthOf("a"). If the min length of a is in the correct relationship with the value on
 *       the right hand side, then the assignment is legal.
 *   <li> When checking whether an array access is legal, the UBC first checks the upper bound type
 *       of the index. If that fails, it checks if the index is a compile time constant. If it is,
 *       then it queries the MinLen Checker to determine if the array is longer than the value of
 *       the constant. If it is, the access is safe.
 * </ul>
 *
 * <p>SameLen dependencies in the UBC:
 *
 * <ul>
 *   <li> When checking whether an array access is legal, the UBC first checks the upper bound type.
 *       If it's an LTL (or LTOM/LTEL), then it collects, from the SameLen Checker, the list of
 *       arrays that are known to be the same length as the array being accessed. Then the
 *       annotation is checked to see if it is valid for any of the arrays in question.
 * </ul>
 *
 * <p>Lower Bound dependencies in UBC:
 *
 * <ul>
 *   <li> When an array is created with length equal to the sum of two quantities, if one of the
 *       quantities is non-negative, the other becomes LTEL of the new array. If one is positive,
 *       the other becomes LTL.
 *   <li> When a non-negative is subtracted from an LTL, it stays LTL.
 * </ul>
 *
 * <p>Proposed possible dependencies:
 *
 * <p>The MinLen Checker would like to depend on the Lower Bound Checker. This would allow the
 * following case:
 *
 * <ul>
 *   <li> When an array is created with a positive length argument, the array has @MinLen(1).
 * </ul>
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public class IndexChecker extends UpperBoundChecker {}
