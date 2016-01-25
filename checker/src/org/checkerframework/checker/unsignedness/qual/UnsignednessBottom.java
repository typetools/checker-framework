package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * UnsignednessBottom is the bottom qualifier in the Unsigned Type
 * System, and is only assigned to a value in error.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf( { Constant.class } )
public @interface UnsignednessBottom { }
