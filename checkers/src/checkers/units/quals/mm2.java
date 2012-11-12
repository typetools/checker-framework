package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Square millimeter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Area.class)
public @interface mm2 {}
