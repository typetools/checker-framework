package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

import javax.lang.model.type.TypeKind;

/**
 * UnsignednessBottom is the bottom qualifier in the Unsigned Type
 * System, and is only assigned to a value in error.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf( { Constant.class } )
@ImplicitFor(
    literals = { LiteralKind.NULL },
    types = { TypeKind.NULL }
    )
public @interface UnsignednessBottom { }
