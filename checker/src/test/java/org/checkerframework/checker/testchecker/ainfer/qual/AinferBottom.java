package org.checkerframework.checker.testchecker.ainfer.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Toy type system for testing field inference.
 *
 * @see AinferSibling1, AinferSibling2, AinferParent
 */
@SubtypeOf({AinferImplicitAnno.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface AinferBottom {}
