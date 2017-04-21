package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type is exactly the same as an {@link IntRange} annotation with the same
 * values. However, this annotation is derived from an {@link
 * org.checkerframework.checker.index.qual.Positive} annotation from the {@link
 * org.checkerframework.checker.index.IndexChecker}.
 *
 * <p>IntRangeFromPositive annotations derived from Positive annotations are used to create IntRange
 * annotations, but IntRangeFromPositive annotations are not checked when they appear on the left
 * hand side of expressions. Therefore, the Index Checker MUST be run on any code with @Positive
 * annotations on the left-hand side of expressions, since the Value Checker will derive information
 * from them but not check them.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SubtypeOf(UnknownVal.class)
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface IntRangeFromPositive {}
