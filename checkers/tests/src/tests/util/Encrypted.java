package tests.util;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.*;

/**
 * Denotes an object with a representation that has been encrypted.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Target(ElementType.TYPE_USE)
public @interface Encrypted {}
