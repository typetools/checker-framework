package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 *  Degree Celsius.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Temperature.class)
public @interface C {}
