package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Meter.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Length.class)
// This is the default:
// @UnitsRelations(org.checkerframework.checker.units.UnitsRelationsDefault.class)
// If you want an alias for "m", e.g. "Meter", simply create that
// annotation and add this meta-annotation:
// @UnitsMultiple(quantity=m.class, prefix=Prefix.one)
@SuppressWarnings("checkstyle:typename")
public @interface m {
  Prefix value() default Prefix.one;
}
