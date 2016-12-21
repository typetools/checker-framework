package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating that the annotated target should be a byte, short, char, int, or long in
 * the given range.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SubtypeOf(UnknownVal.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface IntRange {
    /** Smallest value, inclusive */
    long from() default Long.MIN_VALUE;
    /** Largest value, inclusive */
    long to() default Long.MAX_VALUE;
}
