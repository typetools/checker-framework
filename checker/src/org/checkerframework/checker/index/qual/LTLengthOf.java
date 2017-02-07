package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose value is less than the lengths of all the
 * given sequences.
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than both {@code
 * a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have different
 * lengths.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(LTEqLengthOf.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LTLengthOf {
    /** Sequences, each of which is longer than the annotated expression's value. */
    @JavaExpression
    public String[] value();
}
