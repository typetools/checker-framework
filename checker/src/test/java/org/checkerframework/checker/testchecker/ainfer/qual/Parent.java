package org.checkerframework.checker.testchecker.ainfer.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Toy type system for testing field inference.
 *
 * @see Sibling1, Sibling2, Parent
 */
@SubtypeOf({DefaultType.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Parent {}
