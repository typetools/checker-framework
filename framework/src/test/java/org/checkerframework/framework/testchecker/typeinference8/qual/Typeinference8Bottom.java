package org.checkerframework.framework.testchecker.typeinference8.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom qualifier of the trivial type system used by {@link
 * org.checkerframework.framework.testchecker.typeinference8.Typeinference8InvariantChecker}.
 */
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Typeinference8Top.class)
public @interface Typeinference8Bottom {}
