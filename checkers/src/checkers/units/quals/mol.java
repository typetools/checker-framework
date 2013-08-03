package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Mole (unit of {@link Substance}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Substance.class)
public @interface mol {
    Prefix value() default Prefix.one;
}
