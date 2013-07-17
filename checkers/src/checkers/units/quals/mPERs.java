package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Meter per second.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Speed.class)
public @interface mPERs {
    Prefix value() default Prefix.one;
}
