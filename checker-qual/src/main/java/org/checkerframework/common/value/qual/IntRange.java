package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type evaluates to an integral value (byte, short, char, int, or long) in
 * the given range. The bounds are inclusive. For example, the following declaration allows the 12
 * values 0, 1, ..., 11:
 *
 * <pre>{@code @IntRange(from=0, to=11) int month;}</pre>
 *
 * <p>If only one of the {@code to} and {@code from} fields is set, then the other will default to
 * the max/min value of the type of the variable that is annotated. (In other words, the defaults
 * {@code Long.MIN_VALUE} and {@code Long.MAX_VALUE} are used only for {@code long}; appropriate
 * values are used for other types.)
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@SubtypeOf(UnknownVal.class)
public @interface IntRange {
  /**
   * Largest value in the range, inclusive.
   *
   * @return the largest value in the range, inclusive
   */
  long from() default Long.MIN_VALUE;

  /**
   * Largest value in the range, inclusive.
   *
   * @return the largest value in the range, inclusive
   */
  long to() default Long.MAX_VALUE;
}
