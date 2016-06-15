package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value is to be interpreted as unsigned.
 * That is, if the most significant bit in the bitwise representation is
 * set, then the bits should be interpreted as a negative number.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf( { UnknownSignedness.class } )
@ImplicitFor(
    types = {
        TypeKind.BYTE,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.SHORT,
        TypeKind.FLOAT,
        TypeKind.DOUBLE,
        TypeKind.CHAR
    }

    // This is commented out until implicitly signed boxed types are implemented
    // correctly.

    /*,
    typeNames = {
        java.lang.Byte.class,
        java.lang.Short.class,
        java.lang.Integer.class,
        java.lang.Long.class
    }*/ )
public @interface Signed { }
