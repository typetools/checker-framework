package org.checkerframework.checker.units.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Define the relation between a base unit and the current unit.
 *
 * <p>TODO: add support for factors and more general formulas? E.g. it would be cool if the relation
 * hour &rarr; minute and Fahrenheit &rarr; Celsius could be expressed.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitsMultiple {
  /**
   * Returns the base unit to use.
   *
   * @return the base unit to use
   */
  Class<? extends Annotation> quantity();

  /**
   * Returns the scaling prefix.
   *
   * @return the scaling prefix
   */
  Prefix prefix() default Prefix.one;
}
