package tests.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Toy type system for testing reflection resolution.
 * Uses org.checkerframework.framework.qual.Bottom as bottom
 * @see Sibling1, Sibling2
 */
@TypeQualifier
@SubtypeOf({Sibling1.class, Sibling2.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(DefaultLocation.LOWER_BOUNDS)
public @interface ReflectBottom {}

