package tests.util;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** A subtype of SuperQual. */
@TypeQualifier
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SubQual {}
