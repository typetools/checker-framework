package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The value of the annotated expression is a sequence containing at least the given number of
 * elements. An alias for an {@link ArrayLenRange} annotation with a {@code from} field and the
 * maximum possible value for an array length ({@code Integer.MAX_VALUE}) as its {@code to} field.
 *
 * <p>This annotation is used extensively by the Index Checker.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MinLen {
  /** The minimum number of elements in this sequence. */
  int value() default 0;
}
