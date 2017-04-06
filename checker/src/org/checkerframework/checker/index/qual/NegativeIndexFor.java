package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression is between {@code -1} and {@code -a.length - 1}, inclusive, for each
 * sequence {@code a} listed in the annotation.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SearchIndexFor.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NegativeIndexFor {
    /**
     * Sequences for which this value is a "negative index"; that is, the expression is in the range
     * {@code -1} to {@code -a.length - 1}, inclusive, for each sequence {@code a} given here.
     */
    @JavaExpression
    public String[] value();
}
