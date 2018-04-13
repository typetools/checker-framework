package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The bottom type in the Signature String type system. Programmers should rarely write this type.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@SubtypeOf({Identifier.class, FieldDescriptorForArray.class, MethodDescriptor.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@ImplicitFor(literals = LiteralKind.NULL, typeNames = java.lang.Void.class)
@DefaultFor({TypeUseLocation.LOWER_BOUND})
public @interface SignatureBottom {}
