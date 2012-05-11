package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Mole.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Substance.class)
public @interface mol {
    Prefix value() default Prefix.one;
}
