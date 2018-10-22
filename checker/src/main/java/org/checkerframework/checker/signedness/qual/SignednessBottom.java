package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom type in the Unsigned type system. Programmers should rarely write this type.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@SubtypeOf({Constant.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@ImplicitFor(
        literals = LiteralKind.NULL,
        types = {TypeKind.NULL})
public @interface SignednessBottom {}
