package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * {@link UnsignedBottom} is the bottom qualifier in the Unsigned Type
 * System, and is only assigned to a value in error.
 */

@TypeQualifier
@Target({})
@SubtypeOf({Constant.class})
public @interface UnsignednessBottom {}
