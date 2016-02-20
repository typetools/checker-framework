package org.checkerframework.checker.igj;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.checker.igj.qual.I;
import org.checkerframework.checker.igj.qual.Immutable;
import org.checkerframework.checker.igj.qual.Mutable;
import org.checkerframework.framework.qual.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.Tree.Kind;

/**
 * An annotation used to represent a placeholder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 */
@SubtypeOf({Mutable.class, Immutable.class, I.class})
@ImplicitFor(
        trees = { Kind.NULL_LITERAL },
        typeClasses = { AnnotatedPrimitiveType.class }
)
@DefaultFor({ TypeUseLocation.LOWER_BOUND })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
@interface IGJBottom {}
