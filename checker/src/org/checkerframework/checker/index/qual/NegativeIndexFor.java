package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression is both negative and has absolute value less than the length of each
 * sequence listed in the annotation. Used (with SearchIndex) to handle the JDK's binary search
 * routine.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SearchIndex.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NegativeIndexFor {
    /** Sequences, each of which is longer than the annotated expression's absolute value. */
    @JavaExpression
    public String[] value();
}
