package checkers.units.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * MixedUnits is the result of multiplying or dividing units, where no more
 * specific unit is known from a UnitsRelations implementation.
 */
@TypeQualifier
@SubtypeOf(UnknownUnits.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MixedUnits {}
