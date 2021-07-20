package org.checkerframework.checker.interning.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a variable has been interned, i.e., that the variable refers to the canonical
 * representation of an object.
 *
 * <p>To specify that all objects of a given type are interned, annotate the class declaration:
 *
 * <pre>
 *   public @Interned class MyInternedClass { ... }
 * </pre>
 *
 * This is equivalent to annotating every use of MyInternedClass, in a declaration or elsewhere. For
 * example, enum classes are implicitly so annotated.
 *
 * @see org.checkerframework.checker.interning.InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownInterned.class)
@QualifierForLiterals({LiteralKind.PRIMITIVE, LiteralKind.STRING}) // everything but NULL
@DefaultFor(
        typeKinds = {
            TypeKind.BOOLEAN,
            TypeKind.BYTE,
            TypeKind.CHAR,
            TypeKind.DOUBLE,
            TypeKind.FLOAT,
            TypeKind.INT,
            TypeKind.LONG,
            TypeKind.SHORT
        })
public @interface Interned {}
