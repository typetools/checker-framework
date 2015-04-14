package tests.reflection.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TypeQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Toy type system for testing reflection resolution.
 * Uses org.checkerframework.framework.qual.Bottom as bottom
 * @see Sibling1, Sibling2
 */
@TypeQualifier
@PolymorphicQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyReflection {}

