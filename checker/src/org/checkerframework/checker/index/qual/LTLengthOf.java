package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose value is less than the lengths of all the
 * given sequences. This annotation is rarely used; it is more common to use {@code @}{@link
 * IndexFor}.
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than both {@code
 * a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have different
 * lengths.
 *
 * <p>The {@code @LTLengthOf} annotation takes an optional {@code offset} element. If it is
 * nonempty, then the annotated expression plus the expression in {@code offset[i]} is less than the
 * length of the sequence specified by {@code value[i]}.
 *
 * <p>For example, suppose expression {@code e} has type {@code @LTLengthOf(value = {"a", "b"},
 * offset = {"-1", "x"})}. Then {@code e - 1} is less than {@code a.length}, and {@code e + x} is
 * less than {@code b.length}.
 *
 * @see IndexFor
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
     * This expression plus the annotated expression is less than the length of the sequence. The
     * {@code offset} element must ether be empty or the same length as {@code value}.
     *
     * <p>The expressions in {@code offset} may be addition/subtraction of any number of Java
     * expressions. For example, {@code @LessThanLengthOf(value = "a", offset = "x + y + 2"}}.
     */
    @JavaExpression
    String[] offset() default {};
}
