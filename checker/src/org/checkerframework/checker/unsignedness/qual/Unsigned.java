package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value is to be interpreted as unsigned.
 * That is, if the most significant bit in the bitwise representation is
 * set, then the bits should be interpreted as a large positive number
 * rather than as a negative number.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf( { UnknownSignedness.class } )
public @interface Unsigned { }
