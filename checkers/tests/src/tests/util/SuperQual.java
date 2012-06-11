package tests.util;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.*;

/** A supertype of SubQual. */
@TypeQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target(ElementType.TYPE_USE)
public @interface SuperQual {}
