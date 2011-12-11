package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Ampere.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Current.class)
public @interface A {
    Prefix value() default Prefix.one;
}