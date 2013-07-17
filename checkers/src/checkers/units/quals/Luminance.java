package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Units of luminance.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Unqualified.class)
// TODO: is Luminance the correct term? Or is it Luminosity? Or Luminous Intensity?
public @interface Luminance {}
