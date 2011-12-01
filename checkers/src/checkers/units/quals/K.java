package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Kelvin.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Temperature.class)
public @interface K {
    Prefix value() default Prefix.one;
}
