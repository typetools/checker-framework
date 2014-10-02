package org.checkerframework.checker.units.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * Meter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(Length.class)
// This is the default:
// @UnitsRelations(org.checkerframework.checker.units.UnitsRelationsDefault.class)
// If you want an alias for "m", e.g. "Meter", simply create that
// annotation and add this meta-annotation:
// @UnitsMultiple(quantity=m.class, prefix=Prefix.one)
public @interface m {
    Prefix value() default Prefix.one;
}
