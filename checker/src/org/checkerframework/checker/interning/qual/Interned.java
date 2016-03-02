package org.checkerframework.checker.interning.qual;

import org.checkerframework.checker.interning.InterningChecker;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

/**
 * Indicates that a variable has been interned, i.e., that the variable refers
 * to the canonical representation of an object.
 * <p>
 *
 * To specify that all objects of a given type are interned, annotate the class declaration:
 * <pre>
 *   public @Interned class MyInternedClass { ... }
 * </pre>
 * This is equivalent to annotating every use of MyInternedClass, in a
 * declaration or elsewhere.  For example, enum classes are implicitly so
 * annotated.
 * <p>
 *
 * This annotation is associated with the {@link InterningChecker}.
 *
 * @see InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@SubtypeOf(UnknownInterned.class)
@ImplicitFor(literals = { LiteralKind.ALL },
    types = { TypeKind.BOOLEAN, TypeKind.BYTE,
              TypeKind.CHAR, TypeKind.DOUBLE,
              TypeKind.FLOAT, TypeKind.INT,
              TypeKind.LONG, TypeKind.SHORT },
    typeNames = { Void.class })
@DefaultFor(value={ TypeUseLocation.LOWER_BOUND } )
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Interned {}
