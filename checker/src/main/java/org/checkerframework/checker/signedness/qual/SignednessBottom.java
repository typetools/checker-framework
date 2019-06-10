package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom type in the Signedness type system. Programmers should rarely write this type.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@SubtypeOf({SignednessEither.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface SignednessBottom {}
