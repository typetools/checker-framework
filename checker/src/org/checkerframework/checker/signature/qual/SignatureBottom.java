package org.checkerframework.checker.signature.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Represents the bottom of the type-qualifier hierarchy.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({Identifier.class,
    FieldDescriptorForArray.class,
    MethodDescriptor.class
    })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
@ImplicitFor(literals = { LiteralKind.NULL },
  typeNames = {java.lang.Void.class})
@DefaultFor({ TypeUseLocation.LOWER_BOUND })
public @interface SignatureBottom {}
