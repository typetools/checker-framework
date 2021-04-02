package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to an integer whose value is less than or equal to the lengths
 * of all the given sequences. ("LTEq" stands for "Less than or equal to".)
 *
 * <p>For example, an expression with type {@code @LTLengthOf({"a", "b"})} is less than or equal to
 * both {@code a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have
 * different lengths.
 *
 * <p>{@code @LTEqLengthOf({"a"})} = {@code @LTLengthOf(value={"a"}, offset=-1)}, and<br>
 * {@code @LTEqLengthOf(value={"a"}, offset=x)} = {@code @LTLengthOf(value={"a"}, offset=x-1)} for
 * any x.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UpperBoundUnknown.class)
public @interface LTEqLengthOf {
  /** Sequences, each of which is at least as long as the annotated expression's value. */
  @JavaExpression
  public String[] value();
}
