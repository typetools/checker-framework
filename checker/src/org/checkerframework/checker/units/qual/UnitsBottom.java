package org.checkerframework.checker.units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * UnitsBottom is the bottom type of the type hierarchy.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@SubtypeOf({}) // needs to be done programmatically
@Documented
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor(TypeUseLocation.LOWER_BOUND)
@ImplicitFor(typeNames = Void.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnitsBottom {}
