package org.checkerframework.checker.index.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated expression evaluates to an integer greater than or equal to 1.
 *
 * <p>As an example of a use-case for this type, consider the following code:
 *
 * <pre>{@code
 * if (arr.length > 0) {
 *    int j = arr[arr.length - 1];
 * }
 * }</pre>
 *
 * Without the knowing that {@code arr.length} is positive, the Index Checker cannot verify that
 * accessing the last element of the array is safe - there might not be a last element!
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({NonNegative.class})
public @interface Positive {}
