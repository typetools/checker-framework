package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to either -1 or a value compatible with a {@link LTLengthOf}
 * annotation with the same parameters-
 *
 * <p>This is the return type of {@link java.lang.String#indexOf(String) String.indexOf} and {@link
 * java.lang.String#lastIndexOf(String) String.lastIndexOf} in the JDK.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(IndexOfUnknown.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface IndexOfIndexFor {
    /** Sequences, each of which is longer than the annotated expression's value. */
    @JavaExpression
    public String[] value();

    /**
     * If the annotated expression is not -1, then this expression plus the annotated expression is
     * less than the length of the sequence. The {@code offset} element must ether be empty or the
     * same length as {@code value}.
     *
     * <p>The expressions in {@code offset} may be addition/subtraction of any number of Java
     * expressions. For example, {@code @IndexOfIndexFor(value = "a", offset = "b.length() - 1")}.
     */
    @JavaExpression
    public String[] offset();
}
