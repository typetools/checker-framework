package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression is between -1 and -a.length - 1, inclusive, of each sequence a listed in
 * the annotation. Used (with {@link SearchIndex}) to handle the JDK's {@link
 * java.util.Arrays#binarySearch(Object[],Object) binary search} routine.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SearchIndex.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NegativeIndexFor {
    /**
     * Sequences for which this value is a negative index; that is, the expression is in the range
     * -1 to -1 * a.length - 1, inclusive, for each sequence a.
     */
    @JavaExpression
    public String[] value();
}
