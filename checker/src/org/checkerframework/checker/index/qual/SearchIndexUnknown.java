package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top type for the SearchIndex type system. This indicates that the Index checker does not know
 * any arrays that this integer is a {@link SearchIndexFor search index} for.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SearchIndexUnknown {}
