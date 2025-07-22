package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OwningCollection} is a resource collection/array, which definitely
 * owns the underlying collection/array. It can add or remove elements from the collection/array.
 *
 * <p>This annotation can be enforced by running the Resource Leak Checker. The dataflow analysis
 * running after the type checker tracks at least one owner per underlying resource
 * collection/array. In particular, if an expression has type {@code @OwningCollection}, it is also
 * tracked as an owner by the dataflow analysis, which checks that it (or one of its aliases) calls
 * the methods in the {@code @MustCall} type of its elements on all of its elements before leaving
 * scope, or it passes on the obligation (by writing to an {@code @OwningCollection}, passing to an
 * {@code @OwningCollection} parameter, or returning as an {@code @OwningCollection} return type).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({NotOwningCollection.class})
public @interface OwningCollection {}
