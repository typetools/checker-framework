package org.checkerframework.checker.index.qual;

/**
 * An integer that, for each of the given sequences, is either a valid index or is equal to the
 * sequence's length.
 *
 * <p>Writing {@code @IndexOrHigh("arr")} is equivalent to writing {@link NonNegative @NonNegative}
 * {@link LTEqLengthOf @LTEqLengthOf("arr")}, and that is how it is treated internally by the
 * checker. Thus, if you write an {@code @IndexFor("arr")} annotation, you might see warnings about
 * {@code @NonNegative} or {@code @LTEqLengthOf}.
 *
 * @see NonNegative
 * @see LTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
public @interface IndexOrHigh {
    /**
     * Sequences that the annotated expression is a valid index for or is equal to the lengeth of.
     */
    String[] value() default {};
}
