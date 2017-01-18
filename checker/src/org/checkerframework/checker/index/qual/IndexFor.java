package org.checkerframework.checker.index.qual;

/**
 * An integer that can be used to index any of the given sequences.
 *
 * <p>For example, an expression with type {@code @IndexFor({"a", "b"})} is non-negative and is less
 * than both {@code a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have
 * different lengths.
 *
 * <p>Writing {@code @IndexFor("arr")} is equivalent to writing {@link NonNegative @NonNegative}
 * {@link LTLengthOf @LTLengthOf("arr")}, and that is how it is treated interlally by the checker.
 * This if you write an {@code @IndexFor("arr")} annotation, you might see warnings about
 * {@code @NonNegative} or {@code @LTLengthOf}.
 *
 * @see NonNegative
 * @see LTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
public @interface IndexFor {
    String[] value() default {};
}
