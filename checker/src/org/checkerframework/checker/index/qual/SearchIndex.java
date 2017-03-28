package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose between -a.length - 1 and a.length - 1,
 * inclusive, for all sequences a listed in the annotation.
 *
 * <p>This type is designed with one goal: to handle the convention of {@link
 * java.util.Arrays#binarySearch(Object[],Object) binary search} in the JDK, which returns an
 * integer of this type. Because binary search is common in practice, this type exists only to
 * handle that particular usage.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SearchIndexUnknown.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SearchIndex {
    /**
     * Sequences for which the annotated expression is the result of a call to the JDK's {@link
     * java.util.Arrays#binarySearch(Object[],Object) binary search} method.
     */
    @JavaExpression
    public String[] value();
}
