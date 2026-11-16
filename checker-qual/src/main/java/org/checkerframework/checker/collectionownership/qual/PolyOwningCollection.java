package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Collection Ownership type system.
 *
 * <p>Only one reference to a collection may be owning at a time. So if the argument of a method
 * that returns {@code @PolyOwningCollection}, such as {@code Iterable.iterator()}, is
 * {@code @OwningCollection} or {@code @OwningCollectionWithoutObligation}, the return value is
 * {@code @NotOwningCollection}. If the argument's elements are not resources, as in {@code
 * List<String>}, the argument and the return value are both {@link OwningCollectionBottom}.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 * @see NotOwningCollection
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(NotOwningCollection.class)
public @interface PolyOwningCollection {}
