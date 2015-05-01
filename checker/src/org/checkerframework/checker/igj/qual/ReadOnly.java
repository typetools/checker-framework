package org.checkerframework.checker.igj.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Indicates that the annotated reference is a ReadOnly reference.
 *
 * A {@code ReadOnly} reference could refer to a Mutable or an Immutable
 * object. An object may not be mutated through a read only reference,
 * except if the field is marked {@code Assignable}. Only a method with a
 * readonly receiver can be called using a readonly reference.
 *
 * @checker_framework.manual #igj-checker IGJ Checker
 */
@TypeQualifier
@SubtypeOf({})
// TODO: Would these make sense? Some tests break with them.
// @ImplicitFor(types={TypeKind.BOOLEAN, TypeKind.BYTE, TypeKind.CHAR,
//        TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.INT, TypeKind.LONG,
//        TypeKind.SHORT})
@DefaultFor({ DefaultLocation.LOCAL_VARIABLE, DefaultLocation.RESOURCE_VARIABLE,
    DefaultLocation.IMPLICIT_UPPER_BOUNDS })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ReadOnly {}
