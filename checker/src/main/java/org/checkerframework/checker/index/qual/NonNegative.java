package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer greater than or equal to 0.
 *
 * <p>Consider the following example, from a collection that wraps an array. This constructor
 * creates the {@code delegate} array, which must have a non-negative size.
 *
 * <pre>{@code
 * ArrayWrapper(@NonNegative int size) { delegate = new Object[size]; }
 *
 * }</pre>
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf({GTENegativeOne.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NonNegative {}
