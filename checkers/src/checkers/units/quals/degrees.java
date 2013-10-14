package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Degrees.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Angle.class)
public @interface degrees {
}
