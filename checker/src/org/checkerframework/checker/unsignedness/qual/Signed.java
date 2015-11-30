package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * {@link Signed} is a type qualifier that indicates that a value's 
 * signedness is signed.
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
