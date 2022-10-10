package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An integer that can be used to index any of the given sequences.
 *
 * <p>For example, an expression with type {@code @IndexFor({"a", "b"})} is non-negative and is less
 * than both {@code a.length} and {@code b.length}. The sequences {@code a} and {@code b} might have
 * different lengths.
 *
 * <p>The <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html#charAt(int)">
 * {@code String.charAt(int)}</a> method is declared as
 *
 * <pre>{@code
 * class String {
 *   char charAt(@IndexFor("this") index) { ... }
 * }
 * }</pre>
 *
 * <p>Writing {@code @IndexFor("arr")} is equivalent to writing {@link NonNegative @NonNegative}
 * {@link LTLengthOf @LTLengthOf("arr")}, and that is how it is treated internally by the checker.
 * Thus, if you write an {@code @IndexFor("arr")} annotation, you might see warnings about
 * {@code @NonNegative} or {@code @LTLengthOf}.
 *
 * @see NonNegative
 * @see LTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface IndexFor {
  /** Sequences that the annotated expression is a valid index for. */
  String[] value();
}
