package checkers.units.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Meter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@TypeQualifier
@SubtypeOf(Length.class)
// This is the default:
// @UnitsRelations(checkers.units.UnitsRelationsDefault.class)
// If you want an alias for "m", e.g. "Meter", simply create that
// annotation and add this meta-annotation:
// @UnitsMultiple(quantity=m.class, prefix=Prefix.one)
public @interface m {
    Prefix value() default Prefix.one;
}
