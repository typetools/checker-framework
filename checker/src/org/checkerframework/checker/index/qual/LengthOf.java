package org.checkerframework.checker.index.qual;

/**
 * An integer that, for each of the given sequences, is equal to the sequence's length. This is
 * treated as an {@link IndexOrHigh} annotation internally.
 *
 * @see IndexOrHigh
 * @checker_framework.manual #index-checker Index Checker
 */
public @interface LengthOf {
    /** Sequences that the annotated expression is equal to the lengeth of. */
    String[] value() default {};
}
