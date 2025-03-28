package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OwningCollectionWithoutObligation} is a resource collection/arary,
 * which definitely owns the underlying collection/array and has definitely called all of the
 * methods in the {@code @MustCall} type of its elements on all of its elements.
 *
 * <p>This annotation exists such that for a destructor method {@code d} of a class {@code C} with
 * an {@code @OwningCollection} field {@code f}, the post-condition of said destructor method can be
 * of the form {@code @EnsuresQualifier(expression = "this.f", qualifier =
 * OwningCollectionWithoutObligation.class)}.
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
@SubtypeOf({OwningCollection.class})
public @interface OwningCollectionWithoutObligation {}
