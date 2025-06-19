package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Represents a reference to a collection that must not be used to shrink it (e.g., remove or clear),
 * though other references might be able to do so. This is the most general qualifier in the hierarchy.
 *
 * This is the default qualifier in the GrowShrink index hierarchy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
@TypeQualifier
public @interface UnshrinkableRef {}
