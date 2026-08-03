package org.checkerframework.framework.testchecker.typeinference8.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier of the trivial type system used by {@link
 * org.checkerframework.framework.testchecker.typeinference8.Typeinference8InvariantChecker}. The
 * type system is irrelevant to what that checker tests; it exists only because every {@code
 * BaseTypeChecker} needs a qualifier hierarchy.
 */
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface Typeinference8Top {}
