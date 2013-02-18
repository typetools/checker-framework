package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Square meter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Area.class)
public @interface m2 {
    // does this make sense? Is it multiple of (m^2)? Or (multiple of m)^2?
    Prefix value() default Prefix.one;
}
