package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The bottom qualifier in the GrowShrink hierarchy.
 * This is a subtype of both GrowOnly and UncheckedShrinkable.
 * Not intended to be written by users.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({GrowOnly.class, UncheckedShrinkable.class})
@TypeQualifier
public @interface BottomGrowShrink {}
