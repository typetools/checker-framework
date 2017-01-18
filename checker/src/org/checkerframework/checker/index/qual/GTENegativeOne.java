package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer greater than or equal to -1. ("GTE" stands for
 * ``Greater Than or Equal to''.)
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf({LowerBoundUnknown.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface GTENegativeOne {}
