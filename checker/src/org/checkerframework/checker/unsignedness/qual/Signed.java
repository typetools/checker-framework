package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Signed is a type qualifier which indicates that a value should
 * be interpreted using two's complement encoding, and should function
 * normally in Java.
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
    } )
public @interface Signed { }
