package org.checkerframework.checker.lowerbound.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.checker.lowerbound.qual.NonNegative;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * In the Lower Bound Checker's type system, this type
 * represents any integer greater than or equal to 1.
 * The Lower Bound Checker is a subchecker of the Index
 * Checker, which checks for ArrayIndexOutOfBoundsExceptions.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf({NonNegative.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Positive {}
