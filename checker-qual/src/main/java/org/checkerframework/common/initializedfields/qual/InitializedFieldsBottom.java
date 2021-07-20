package org.checkerframework.common.initializedfields.qual;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The bottom type qualifier for the Initialized Fields type system. It is the type of {@code null}.
 * Programmers should rarely write this qualifier.
 *
 * @checker_framework.manual #initialized-fields-checker Initialized Fields Checker
 */
@SubtypeOf({InitializedFields.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface InitializedFieldsBottom {}
