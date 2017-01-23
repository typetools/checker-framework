package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * The annotated expression evaluates to an integer whose value is less than or equal to the lengths
 * of all the given sequences. ("LTEq" stands for "Less than or equal to".)
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than or equal to
 * both {@code a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have
 * different lengths.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(UpperBoundUnknown.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LTEqLengthOf {
    /** Sequences, each of which is at least as long as the annotated expression's value. */
    public String[] value();
}
