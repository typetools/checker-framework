package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

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
@DefaultFor({
  // TypeUseLocation.ALL,
  // TypeUseLocation.CONSTRUCTOR_RESULT,
  TypeUseLocation.EXCEPTION_PARAMETER,
  // TypeUseLocation.EXPLICIT_LOWER_BOUND,
  // TypeUseLocation.EXPLICIT_UPPER_BOUND,
  // TypeUseLocation.FIELD,
  // TypeUseLocation.IMPLICIT_LOWER_BOUND,
  // TypeUseLocation.IMPLICIT_UPPER_BOUND,
  TypeUseLocation.LOCAL_VARIABLE,
  // TypeUseLocation.LOWER_BOUND,
  // TypeUseLocation.OTHERWISE,
  // TypeUseLocation.PARAMETER,
  // TypeUseLocation.RECEIVER,
  TypeUseLocation.RESOURCE_VARIABLE,
  TypeUseLocation.RETURN,
  TypeUseLocation.UPPER_BOUND
})
public @interface OwningCollectionBottom {}
