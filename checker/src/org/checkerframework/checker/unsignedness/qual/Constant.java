package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * Constant is a type qualifier which indicates that a value
 * is a compile-time constant, and could be Unsigned or
 * Signed.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf( { Unsigned.class, Signed.class } )
@ImplicitFor(
    literals = {
        LiteralKind.INT,
        LiteralKind.LONG,
        LiteralKind.CHAR
    } )
public @interface Constant { }
