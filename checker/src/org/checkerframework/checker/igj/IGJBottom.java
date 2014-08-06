package org.checkerframework.checker.igj;

import java.lang.annotation.Target;

import org.checkerframework.checker.igj.qual.I;
import org.checkerframework.checker.igj.qual.Immutable;
import org.checkerframework.checker.igj.qual.Mutable;
import org.checkerframework.framework.qual.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import com.sun.source.tree.Tree.Kind;

/**
 * An annotation used to represent a place holder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 */
@TypeQualifier
@SubtypeOf({Mutable.class, Immutable.class, I.class})
@ImplicitFor(
        trees = { Kind.NULL_LITERAL },
        typeClasses = { AnnotatedPrimitiveType.class }
)
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@Target({}) // empty target prevents programmers from writing this in a program
@interface IGJBottom {}
