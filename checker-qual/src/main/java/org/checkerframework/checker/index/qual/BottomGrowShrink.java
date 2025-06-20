package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom qualifier in the GrowShrink hierarchy. This is a subtype of both GrowOnly and
 * UncheckedShrinkable. Not intended to be written by users.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({GrowOnly.class, UncheckedShrinkable.class})
public @interface BottomGrowShrink {}
