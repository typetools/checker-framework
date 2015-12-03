package org.checkerframework.checker.oigj;

import org.checkerframework.checker.oigj.qual.I;
import org.checkerframework.checker.oigj.qual.Immutable;
import org.checkerframework.checker.oigj.qual.Modifier;
import org.checkerframework.checker.oigj.qual.Mutable;
import org.checkerframework.checker.oigj.qual.O;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeQualifier;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

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
@SubtypeOf({Mutable.class, Immutable.class, I.class,
    Modifier.class, O.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
@ImplicitFor(
        trees = { Kind.NULL_LITERAL, Kind.CLASS, Kind.ENUM,
                Kind.INTERFACE, Kind.ANNOTATION_TYPE,
                Kind.NEW_ARRAY },
        typeClasses = { AnnotatedPrimitiveType.class },
        typeNames = { java.lang.Void.class }
)
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@interface OIGJMutabilityBottom { }
