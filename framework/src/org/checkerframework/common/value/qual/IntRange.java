package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type evaluates to an integral value (byte, short, char, int, or long) in
 * the given range. The bounds are inclusive; for example, {@code @IntRange(from=6, to=9)}
 * represents the four values 6, 7, 8, and 9.
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
