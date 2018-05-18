package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression whose type has this annotation evaluates to a value that is a sequence, and that
 * sequence has the same length as the given sequences. For example, if {@code b}'s type is
 * annotated with {@code @SameLen("a")}, then {@code a} and {@code b} have the same length.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SameLenUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SameLen {
    /** A list of other sequences with the same length. */
    @JavaExpression
    String[] value();

    /**
     * When an index for the value[i] is added to offset[i], then the resulting expression is an
     * index for the array that is annotated with this SameLen annotation. The {@code offset}
     * element must ether be empty or the same length as {@code value}. These offets work exactly
     * like those in {@link LTLengthOf}. The default value for the offset is "0", meaning that the
     * length of the annotated array is exactly the length the arrays listed in the value
     * expressions.
     *
     * <p>The expressions in {@code offset} may be addition/subtraction of any number of Java
     * expressions. For example, {@code @SameLen(value = "a", offset = "x + y + 2"}}.
     */
    @JavaExpression
    String[] offset() default {};
}
