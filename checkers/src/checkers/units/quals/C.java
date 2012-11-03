package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 *  Degree Centigrade (Celsius).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Temperature.class)
public @interface C {}
