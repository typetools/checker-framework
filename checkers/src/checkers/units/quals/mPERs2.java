package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Meter per second squared.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Acceleration.class)
public @interface mPERs2 {
    Prefix value() default Prefix.one;
}
