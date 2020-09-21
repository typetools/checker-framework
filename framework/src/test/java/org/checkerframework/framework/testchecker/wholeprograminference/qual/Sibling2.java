package org.checkerframework.framework.testchecker.wholeprograminference.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Toy type system for testing field inference.
 *
 * @see Sibling1, Sibling2, Parent
 */
@SubtypeOf(Parent.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Sibling2 {}
