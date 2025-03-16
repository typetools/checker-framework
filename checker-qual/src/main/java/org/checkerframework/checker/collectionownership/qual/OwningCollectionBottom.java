package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom qualifier of the Collection Ownership type hierarchy, and the default qualifier.
 *
 * <p>An expression of type {@code @OwningCollectionBottom} is either not a collection/array, or a
 * collection/array, whose elements have empty {@code @MustCall} type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({OwningCollectionWithoutObligation.class})
@DefaultQualifierInHierarchy
public @interface OwningCollectionBottom {}
