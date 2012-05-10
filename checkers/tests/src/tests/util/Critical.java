package tests.util;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.*;

/**
 * Denotes an exception that is particularly important.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Target(ElementType.TYPE_USE)
public @interface Critical {}
