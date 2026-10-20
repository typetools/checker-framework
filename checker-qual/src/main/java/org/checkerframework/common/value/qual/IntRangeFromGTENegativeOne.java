package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type is exactly the same as an {@link IntRange} annotation whose {@code
 * from} field is {@code -1} and whose {@code to} field is the maximum value for its type. However,
 * this annotation is derived from an {@code org.checkerframework.checker.index.qual.GTENegativeOne}
 * annotation.
 *
 * <p>The Value Checker trusts this annotation. For soundness, the Index Checker must be run on any
 * code with @GTENegativeOne annotations on the left-hand side of assignments.
 *
 * <p>It is an error to write this annotation directly. {@code @GTENegativeOne}, or an
 * {@code @IntRange} annotation whose {@code from} element is {@code -1} and whose {@code to}
 * element is the maximum value of the annotated type, should always be written instead. This
 * annotation is not retained in bytecode, but is replaced with {@code @UnknownVal}, so that it is
 * not enforced on method boundaries. The {@code @GTENegativeOne} annotation it replaced is retained
 * in bytecode by the Lower Bound Checker instead.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({})
@SubtypeOf(UnknownVal.class)
public @interface IntRangeFromGTENegativeOne {}
