package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Hour.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Time.class)
// TODO: support arbitrary factors?
// @UnitsMultiple(quantity=s.class, factor=3600)
public @interface h {}
