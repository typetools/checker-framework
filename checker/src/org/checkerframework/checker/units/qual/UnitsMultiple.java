package org.checkerframework.checker.units.qual;

import java.lang.annotation.Annotation;

/**
 * Define the relation between a base unit and the current unit.
 *
 * <p>TODO: add support for factors and more general formulas? E.g. it would be cool if the relation
 * hour &rarr; minute and Fahrenheit &rarr; Celsius could be expressed.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
public @interface UnitsMultiple {
    /** @return the base unit to use */
    Class<? extends Annotation> quantity();

    /** @return the scaling prefix */
    Prefix prefix() default Prefix.one;
}
