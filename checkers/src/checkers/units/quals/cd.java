package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Candela (unit of luminance).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Luminance.class)
public @interface cd {
    Prefix value() default Prefix.one;
}
