package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Meter per second.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Speed.class)
public @interface mPERs {
    Prefix value() default Prefix.one;
}
