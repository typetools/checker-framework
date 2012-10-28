package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Gram.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Mass.class)
public @interface g {
    Prefix value() default Prefix.one;
}
