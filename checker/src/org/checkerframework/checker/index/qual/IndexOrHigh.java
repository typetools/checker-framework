package org.checkerframework.checker.index.qual;

/**
 * This annotation is used by the index checker to indicate an integer that is either: safe to use
 * to access each of the variables named in its 'value' field, or exactly equal to the length of one
 * or more of those variables, and safe to use as an index for the rest. Writing @IndexOrHigh("arr")
 * is equivalent to writing {@link
 * org.checkerframework.checker.lowerbound.qual.NonNegative @NonNegative} {@link
 * org.checkerframework.checker.upperbound.qual.LTEqLengthOf @LTEqLengthOf("arr")}; internally, the
 * IndexOrHigh annotation is translated into those two annotations (from the Lower Bound Checker and
 * Upper Bound Checker, respectively), so annotating a variable as @IndexOrHigh might result in
 * warnings about @NonNegative or @LTEqLengthOf.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public @interface IndexOrHigh {
    String[] value() default {};
}
