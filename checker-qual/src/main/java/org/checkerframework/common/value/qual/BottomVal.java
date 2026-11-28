package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom type in the Constant Value type system. Programmers should rarely write this type.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({
  ArrayLen.class,
  BoolVal.class,
  DoubleVal.class,
  IntVal.class,
  StringVal.class,
  MatchesRegex.class,
  DoesNotMatchRegex.class,
  ArrayLenRange.class,
  IntRange.class,
  IntRangeFromPositive.class,
  IntRangeFromGTENegativeOne.class,
  IntRangeFromNonNegative.class
})
@InvisibleQualifier
public @interface BottomVal {}
