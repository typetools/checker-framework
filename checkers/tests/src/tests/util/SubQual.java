package tests.util;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.*;

/** A subtype of SuperQual. */
@TypeQualifier
@SubtypeOf(SuperQual.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SubQual {}
