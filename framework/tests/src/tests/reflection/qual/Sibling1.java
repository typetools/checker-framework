package tests.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Toy type system for testing reflection resolution.
 * Uses org.checkerframework.framework.qual.Bottom as bottom
 * @see Top, Sibling2
 */
@SubtypeOf(tests.reflection.qual.Top.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Sibling1 {}
