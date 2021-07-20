package org.checkerframework.checker.index.qual;

import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated expression evaluates to an integer whose value is at least 2 less than the lengths
 * of all the given sequences.
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than or equal to
 * both {@code a.length-2} and {@code b.length-2}. Equivalently, it is less than both {@code
 * a.length-1} and {@code b.length-1}. The sequences {@code a} and {@code b} might have different
 * lengths.
 *
 * <p>In the annotation's name, "LTOM" stands for "less than one minus".
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(LTLengthOf.class)
public @interface LTOMLengthOf {
    /**
     * Sequences, each of whose lengths is at least 1 larger than the annotated expression's value.
     */
    @JavaExpression
    public String[] value();
}
