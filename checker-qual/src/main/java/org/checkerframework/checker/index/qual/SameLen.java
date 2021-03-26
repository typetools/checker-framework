package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SameLenUnknown.class)
public @interface SameLen {
  /** A list of other sequences with the same length. */
  @JavaExpression
  String[] value();
}
