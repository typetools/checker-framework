package checkers.units.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Units of luminance.
 */
@TypeQualifier
@SubtypeOf(UnknownUnits.class)
// TODO: is Luminance the correct term? Or is it Luminosity? Or Luminous Intensity?
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Luminance {}
