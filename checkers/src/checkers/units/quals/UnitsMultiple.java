package checkers.units.quals;

import java.lang.annotation.Annotation;

/**
 * Define the relation between a base unit and the current unit.
 * TODO: add support for factors and more general formulas?
 * E.g. it would be cool if the relation hour -> minute and
 * Fahrenheit -> Celsius could be expressed.
 */
public @interface UnitsMultiple {
    /**
     * @return The base unit to use.
     */
    Class<? extends Annotation> quantity();

    /**
     * @return The scaling prefix.
     */
    Prefix prefix() default Prefix.one;
}
