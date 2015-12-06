package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
<<<<<<< HEAD
 * Signed is a type qualifier which indicates that a value should
 * be interpreted using two's complement encoding, and should function
 * normally in Java.
=======
 * {@link Signed} is a type qualifier that indicates that a value's 
 * signedness is signed.
>>>>>>> origin/master
 */

@TypeQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownSignedness.class})
@ImplicitFor(
	types = {
		TypeKind.BYTE, 
		TypeKind.INT, 
		TypeKind.LONG, 
		TypeKind.SHORT
	})
public @interface Signed {}
