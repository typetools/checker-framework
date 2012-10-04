package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Minute.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Time.class)
// TODO: @UnitsMultiple(quantity=s.class, factor=60)
public @interface min {}
