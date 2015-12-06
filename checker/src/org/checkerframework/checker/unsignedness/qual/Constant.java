package org.checkerframework.checker.unsignedness.qual;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Constant is a type qualifier which indicates that a value
 * is a compile-time constant, and could be {@link Unsigned} or 
 * {@link Signed}.
 */

@TypeQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Unsigned.class, Signed.class})
@ImplicitFor(
	trees = {
		Tree.Kind.INT_LITERAL,
		Tree.Kind.LONG_LITERAL
	})
public @interface Constant {}
