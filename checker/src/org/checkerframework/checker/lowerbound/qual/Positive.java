package org.checkerframework.checker.lowerbound.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.checker.lowerbound.qual.NonNegative;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * In the Lower Bound Checker's type system, this type
 * represents any integer greater than or equal to 1.
 * The Lower Bound Checker is a subchecker of the Index
 * Checker, which checks for ArrayIndexOutOfBoundsExceptions.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf({NonNegative.class})
//@ImplicitFor(literals = {LiteralKind.NULL},
//	     typeNames = {java.lang.Void.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
//@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
//@DefaultFor({TypeUseLocation.LOWER_BOUND})
public @interface Positive {}
