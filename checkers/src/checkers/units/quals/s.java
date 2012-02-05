package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Second.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Time.class)
public @interface s {
    Prefix value() default Prefix.one;
}
