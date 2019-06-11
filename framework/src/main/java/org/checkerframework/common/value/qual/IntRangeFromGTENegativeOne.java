package org.checkerframework.common.value.qual;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type is exactly the same as an {@link IntRange} annotation whose {@code
 * from} field is {@code -1} and whose {@code to} field is {@code Integer.MAX_VALUE}. However, this
 * annotation is derived from an {@code org.checkerframework.checker.index.qual.GTENegativeOne}
 * annotation.
 *
 * <p>IntRangeFromGTENegativeOne annotations derived from GTENegativeOne annotations are used to
 * create IntRange annotations, but IntRangeFromGTENegativeOne annotations are not checked when they
 * appear on the left hand side of expressions. Therefore, the Index Checker MUST be run on any code
 * with @GTENegativeOne annotations on the left-hand side of expressions, since the Value Checker
 * will derive information from them but not check them.
 *
 * <p>It is an error to write this annotation directly. {@code @GTENegativeOne} or {@code
 * IntRange(from = -1, to = Integer.MAX_VALUE)} should always be written instead. This annotation is
 * not retained in bytecode, but is replaced with {@code @UnknownVal}, so that it is not enforced on
 * method boundaries. The {@code @GTENegativeOne} annotation it replaced is retained in bytecode by
 * the Lower Bound Checker instead.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SubtypeOf(UnknownVal.class)
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface IntRangeFromGTENegativeOne {}
