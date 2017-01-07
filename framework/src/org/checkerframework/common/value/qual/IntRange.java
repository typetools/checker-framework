package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating that the annotated target should be an integral value (byte, short,
 * char, int, or long) in the given range. The bounds are inclusive; for example,
 * {@code @IntRange(from=5, to=8)} represents the 4 values 5, 6, 7, and 8.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SubtypeOf(UnknownVal.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface IntRange {
    /** Smallest value in the range, inclusive */
    long from() default Long.MIN_VALUE;
    /** Largest value in the range, inclusive */
    long to() default Long.MAX_VALUE;
}
