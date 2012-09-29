package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Kilogram.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Mass.class)
@UnitsMultiple(quantity=g.class, prefix=Prefix.kilo)
public @interface kg {}
