package tests.util;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import com.sun.source.tree.Tree.Kind;

/**
 * Denotes an object with a representation that has been encrypted.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@ImplicitFor(trees = { Kind.NULL_LITERAL })
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Encrypted {}
