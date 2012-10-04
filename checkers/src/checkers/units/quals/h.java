package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Hour.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Time.class)
// TODO: support arbitrary factors and +/- for Fahrenheit/Celsius?
// @UnitsMultiple(quantity=s.class, factor=3600)
public @interface h {}
