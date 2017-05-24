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
 * <p>If only one of the {@code to} and {@code from} fields is set, then the other will default to
 * the max/min value of the underlying type of the variable that is annotated. Note that there must
 * be "default" values (Long.MIN_VALUE and Long.MAX_VALUE below) even though they will be replaced
 * in order to prevent javac from issuing warnings when only one of the values is set by a user.
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
