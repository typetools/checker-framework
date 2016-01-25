package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Unsigned is a type qualifier which indicates that a value 
 * is to be interpreted as unsigned, and requires special care.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf( { UnknownSignedness.class } )
public @interface Unsigned { }
