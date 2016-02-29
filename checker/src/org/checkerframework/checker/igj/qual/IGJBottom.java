package org.checkerframework.checker.igj.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

/**
 * An annotation used to represent a placeholder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 */
@SubtypeOf({Mutable.class, Immutable.class, I.class})
@ImplicitFor(literals = { LiteralKind.NULL },
    types = { TypeKind.BOOLEAN, TypeKind.BYTE,
              TypeKind.CHAR, TypeKind.DOUBLE,
              TypeKind.FLOAT, TypeKind.INT,
              TypeKind.LONG, TypeKind.SHORT })
@DefaultFor({ TypeUseLocation.LOWER_BOUND })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
public @interface IGJBottom {}
