package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top type for the Substring Index type system. This indicates that the Index Checker does not
 * know any sequences that this integer is a {@link SubstringIndexFor substring index} for.
 *
 * @checker_framework.manual #index-substringindex Index Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SubstringIndexUnknown {}
