package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Units of substance.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Unqualified.class)
public @interface Substance {}
