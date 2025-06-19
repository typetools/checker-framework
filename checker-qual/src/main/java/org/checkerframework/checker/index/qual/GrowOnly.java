package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * A reference that may only grow the collection. Shrinking operations like remove(), clear() are disallowed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnshrinkableRef.class)
@TypeQualifier
public @interface GrowOnly {}
