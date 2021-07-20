package org.checkerframework.checker.units.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MixedUnits is the result of multiplying or dividing units, where no more specific unit is known
 * from a UnitsRelations implementation.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // forbids a programmer from writing it in a program
@SubtypeOf(UnknownUnits.class)
public @interface MixedUnits {}
