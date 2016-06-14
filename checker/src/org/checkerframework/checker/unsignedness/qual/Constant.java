package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * The valueis a compile-time constant, and could be
 * {@link Signed} or {@link Unsigned}.
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
