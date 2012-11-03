package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Ampere.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Current.class)
public @interface A {
    Prefix value() default Prefix.one;
}
