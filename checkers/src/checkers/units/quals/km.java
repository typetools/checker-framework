package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Kilometer.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Length.class)
@UnitsMultiple(quantity=m.class, prefix=Prefix.kilo)
public @interface km {}
