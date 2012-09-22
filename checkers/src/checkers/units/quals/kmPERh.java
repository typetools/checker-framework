package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Kilometer per hour.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Speed.class)
public @interface kmPERh {}
