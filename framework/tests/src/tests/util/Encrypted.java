package tests.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;
import org.checkerframework.framework.qual.Unqualified;

import com.sun.source.tree.Tree.Kind;

/**
 * Denotes an object with a representation that has been encrypted.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@ImplicitFor(trees = { Kind.NULL_LITERAL })
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Encrypted {}
