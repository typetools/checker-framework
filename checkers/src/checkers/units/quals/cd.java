package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Candela.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Luminance.class)
public @interface cd {
    Prefix value() default Prefix.one;
}
