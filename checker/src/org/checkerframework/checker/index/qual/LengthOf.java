package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * An integer that, for each of the given sequences, is equal to the sequence's length.
 *
 * <p>This is treated as an {@link IndexOrHigh} annotation internally. This is an implementation
 * detail that may change in the future, when this type may be used to implement more precise
 * refinements.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
// Has target of METHOD so that it is stored as a declaration annotation and SameLen Checker can read it.
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER, ElementType.METHOD})
public @interface LengthOf {
    /** Sequences that the annotated expression is equal to the lengeth of. */
    String[] value();
}
