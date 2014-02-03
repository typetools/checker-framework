package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Electric current.
 */
@TypeQualifier
@SubtypeOf(UnknownUnits.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Current {}
