package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type evaluates to an array or a string whose length is in the given
 * range. The bounds are inclusive; for example, {@code @ArrayLenRange(from=6, to=9)} represents an
 * array or a string with four possible values for its length: 6, 7, 8, and 9.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@SubtypeOf(UnknownVal.class)
public @interface ArrayLenRange {
  /**
   * Smallest value in the range, inclusive.
   *
   * @return the smallest value in the range, inclusive
   */
  int from() default 0;

  /**
   * Largest value in the range, inclusive.
   *
   * @return the largest value in the range, inclusive
   */
  int to() default Integer.MAX_VALUE;
}
