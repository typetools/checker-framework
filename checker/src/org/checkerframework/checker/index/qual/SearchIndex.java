package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose absolute value is less than the lengths of
 * all the given sequences.
 *
 * <p>Intentionally does not support offsets, and is not part of the Upper Bound type system. This
 * type is designed with one goal: to handle the convention of binary search in the JDK, which
 * returns an integer of this type. Because binary search is common in practice, this type exists
 * only to handle that particular usage.
 *
 * <p>If you annotate a variable with this type, you should also annotate it with @LTLengthOf of the
 * same arrays.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SearchIndexUnknown.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SearchIndex {
    /** Sequences, each of which is longer than the annotated expression's absolute value. */
    @JavaExpression
    public String[] value();
}
