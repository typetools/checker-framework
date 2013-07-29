package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Square millimeter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Area.class)
public @interface mm2 {}
