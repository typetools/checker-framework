package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Kilometer per hour.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Speed.class)
public @interface kmPERh {}
