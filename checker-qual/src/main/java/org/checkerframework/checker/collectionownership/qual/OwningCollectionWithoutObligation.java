package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OwningCollectionWithoutObligation} is a resource collection/array,
 * which definitely owns the underlying collection/array. Furthermore, every element has already
 * called all of the methods in its {@code @MustCall} type.
 *
 * <p>Consider a destructor method {@code d} of a class {@code C} with an {@code @OwningCollection}
 * field {@code f}. The post-condition of the destructor method is
 * {@code @EnsuresQualifier(expression = "this.f", qualifier =
 * OwningCollectionWithoutObligation.class)}.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({OwningCollection.class})
public @interface OwningCollectionWithoutObligation {}
