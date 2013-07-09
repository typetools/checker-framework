package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A second (1/60 of a minute).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Time.class)
public @interface s {
    Prefix value() default Prefix.one;
}
