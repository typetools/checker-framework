package org.checkerframework.checker.calledmethodsonelements.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom type for the Called Methods On Elements type system.
 *
 * <p>It should rarely be written by a programmer.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@SubtypeOf({CalledMethodsOnElements.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface CalledMethodsOnElementsBottom {}
