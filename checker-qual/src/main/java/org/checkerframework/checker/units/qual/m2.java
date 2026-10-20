package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Square meter.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Area.class)
@SuppressWarnings("checkstyle:typename")
public @interface m2 {
  // Does this make sense? Is it a multiple of (m^2)? Or (multiple of m)^2?
  /**
   * The coefficient that multiplies the unit.
   *
   * @return the coefficient that multiplies the unit
   */
  Prefix value() default Prefix.one;
}
