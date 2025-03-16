package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top qualifier in the Collection Ownership type hierarchy.
 *
 * <p>An expression of type {@code NotOwningCollection} is a resource collection/array, which may
 * not own the underlying collection/array, and thus cannot add or remove elements from it.
 *
 * <p>This annotation can be enforced by running the Resource Leak Checker. It enforces that the
 * expression is not used to add to or remove elements from the underlying collection/array.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NotOwningCollection {}
