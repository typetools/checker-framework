package org.checkerframework.checker.lowerbound.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.checker.lowerbound.qual.GTENegativeOne;

/**
 * In the Lower Bound Checker's type system, this type
 * represents any integer greater than or equal to 0
 *
 * @checker_framework.manual #lowerbound-checker Lower Bound Checker
 */

@SubtypeOf({GTENegativeOne.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NonNegative {}
