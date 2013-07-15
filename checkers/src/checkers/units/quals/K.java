package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Kelvin (unit of temperature).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Temperature.class)
public @interface K {
    Prefix value() default Prefix.one;
}
