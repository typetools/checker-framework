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
 * <p>This type should rarely (if ever) be written by programmers. It is inferred by the
 * SearchIndexChecker when the result of a call to one of the JDK's binary search methods (like
 * {@code Arrays.binarySearch}) is known to be less than zero. For example, consider the following
 * code:
 *
 * <pre>
 *
 *     int index = Arrays.binarySearch(array, target);
 *     if (index &#60; 0) {
 *          // index's type here is &#64;NegativeIndexFor("array")
 *          index = index * -1;
 *          // now index's type is &#64;IndexFor("array")
 *     }
 * </pre>
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
