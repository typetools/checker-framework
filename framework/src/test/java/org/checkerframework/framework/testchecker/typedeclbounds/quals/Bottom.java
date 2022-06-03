package org.checkerframework.framework.testchecker.typedeclbounds.quals;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Toy type system for testing impact of implicit java type conversion.
 *
 * @see Top
 */
@SubtypeOf({S1.class, S2.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(value = {TypeUseLocation.LOWER_BOUND})
public @interface Bottom {}
