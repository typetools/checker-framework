package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OwningCollection} is a resource collection/array that definitely
 * owns the underlying collection/array. It can add or remove elements from the collection/array.
 *
 * <p>The annotated expression (or one of its aliases) must either call the methods in the
 * {@code @MustCall} type of its elements on all of its elements, or pass on the obligation to
 * another expression.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({NotOwningCollection.class})
public @interface OwningCollection {}
