package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code NotOwningCollection} is a non-owning reference to a resource
 * collection/array. Because it does not own the underlying collection/array, it cannot add or
 * remove elements from it.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface NotOwningCollection {}
