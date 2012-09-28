package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Gram.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Mass.class)
public @interface g {
    Prefix value() default Prefix.one;
}
