package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
<<<<<<< HEAD
 * Unsigned is a type qualifier which indicates that a value 
 * is to be interpreted as unsigned, and requires special care.
=======
 * {@link Unsigned} is a type qualifier that indicates that a value's 
 * signedness is unsigned.
>>>>>>> origin/master
 */

@TypeQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownSignedness.class})
public @interface Unsigned {}
