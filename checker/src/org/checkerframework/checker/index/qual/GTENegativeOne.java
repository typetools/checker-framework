package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This type represents any integer greater than or equal to -1.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf({LowerBoundUnknown.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface GTENegativeOne {}
