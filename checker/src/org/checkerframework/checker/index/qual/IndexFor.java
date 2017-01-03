package org.checkerframework.checker.index.qual;

/**
 * This annotation is used by the index checker to indicate an integer that can safely be used to
 * access the variables named in its 'value' field. Writing @IndexFor("arr") is equivalent to
 * writing {@link org.checkerframework.checker.lowerbound.qual.NonNegative @NonNegative} {@link
 * org.checkerframework.checker.upperbound.qual.LTLengthOf @LTLengthOf("arr")}; internally, the
 * IndexFor annotation is translated into those two annotations (from the Lower Bound Checker and
 * Upper Bound Checker, respectively), so annotating a variable as @IndexFor might result in
 * warnings about @NonNegative or @LTLengthOf.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public @interface IndexFor {
    String[] value() default {};
}
