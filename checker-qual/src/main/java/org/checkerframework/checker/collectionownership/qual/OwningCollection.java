package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OwningCollection} is a resource collection that definitely owns the
 * underlying collection. It can add elements to and remove elements from the collection.
 *
 * <p>Unlike {@code @Owning}, which does not restrict aliasing, {@code @OwningCollection} permits at
 * most one owning reference to each resource collection.
 *
 * <p>The annotated expression (or one of its aliases) must either call the methods in the
 * {@code @MustCall} type of its elements on all of its elements or pass the obligation on to
 * another expression.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({NotOwningCollection.class})
public @interface OwningCollection {}
