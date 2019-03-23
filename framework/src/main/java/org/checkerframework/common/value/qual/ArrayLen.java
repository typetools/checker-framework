package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating the length of an array or a string. If an expression's type has this
 * annotation, then at run time, the expression evaluates to an array or a string whose length is
 * one of the annotation's arguments.
 *
 * <p>For example, {@code String @ArrayLen(2) []} is the type for an array of two strings, and
 * {@code String @ArrayLen({1, 2, 4, 8}) []} is the type for an array that contains 1, 2, 4, or 8
 * strings.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SubtypeOf({UnknownVal.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface ArrayLen {
    /** The possible lengths of the array. */
    int[] value();
}
