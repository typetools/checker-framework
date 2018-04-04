package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose length is between {@code -a.length - 1}
 * and {@code a.length - 1}, inclusive, for all sequences {@code a} listed in the annotation.
 *
 * <p>This is the return type of {@link java.util.Arrays#binarySearch(Object[],Object) {@code
 * Arrays.binarySearch}} in the JDK.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SearchIndexUnknown.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SearchIndexFor {
    /**
     * Sequences for which the annotated expression has the type of the result of a call to {@link
     * java.util.Arrays#binarySearch(Object[],Object) {@code Arrays.binarySearch}}.
     */
    @JavaExpression
    public String[] value();
}
