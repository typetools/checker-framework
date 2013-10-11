package tests.util;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Denotes an exception that is particularly important.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Critical {}
