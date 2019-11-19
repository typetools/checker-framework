package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom type in the MethodVal type system. Programmers should rarely write this type.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@InvisibleQualifier
@SubtypeOf({MethodVal.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface MethodValBottom {}
