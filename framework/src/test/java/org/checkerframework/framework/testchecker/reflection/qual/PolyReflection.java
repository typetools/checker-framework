package org.checkerframework.framework.testchecker.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * Toy type system for testing reflection resolution. Uses
 * org.checkerframework.common.subtyping.qual.Bottom as bottom
 *
 * @see Sibling1, Sibling2
 */
@PolymorphicQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyReflection {}
