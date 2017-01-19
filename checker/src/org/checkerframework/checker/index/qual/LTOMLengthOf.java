package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * The annotated expression evaluates to an integer whose value is at least 2 less than the lengths
 * of all the given sequences.
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than or equal to
 * both {@code a.length-2} and {@code b.length-2}. Equivalently, it is less than both {@code
 * a.length-1} and {@code b.length-1}. The sequences {@code a} and {@code b} might have different
 * lengths.
 *
 * <p>In the annotation's name, "LTOM" sndands for "less than one minus".
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(LTLengthOf.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LTOMLengthOf {
    /**
     * Sequences, each of whose lengths is at least 1 larger than than the annotated expression's
     * value.
     */
    public String[] value();
}
