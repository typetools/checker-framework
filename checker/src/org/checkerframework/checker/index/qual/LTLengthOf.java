package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If no offset is specified, then the annotated expression evaluates to an integer whose value is
 * less than the lengths of all the given sequences.
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than both {@code
 * a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have different
 * lengths.
 *
 * <p>If the offset element is nonempty, then the annotated expression plus the expression in
 * offset[i] element is less than the length of the sequence specified by value[i].
 *
 * <p>For example, an expression with type {@code @LTLengthOf(value = {"a", "b"}, offset = {"-1",
 * "x"})} is less than {@code a.length} if "-1" is added to it and is less than {@code b.length} if
 * "x" is added to it.
 *
 * <p>The expressions in offset by be addition/subtraction of any number of Java expressions. For
 * example, {@code @LessThanLengthOf(value = "a", offset = "x + y + 2"}}.
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

    /**
     * This expression plus the annotated expression is less than the length of the sequence. This
     * element must ether be empty or the same size as value.
     */
    @JavaExpression
    String[] offset() default {};
}
