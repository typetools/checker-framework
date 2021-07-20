package org.checkerframework.checker.index.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated expression evaluates to an integer greater than or equal to 0.
 *
 * <p>Consider the following example, from a collection that wraps an array. This constructor
 * creates the {@code delegate} array, which must have a non-negative size.
 *
 * <pre>{@code
 * ArrayWrapper(@NonNegative int size) { delegate = new Object[size]; }
 * }</pre>
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({GTENegativeOne.class})
public @interface NonNegative {}
