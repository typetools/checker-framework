package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An integer that, for each of the given sequences, is either a valid index or is equal to the
 * sequence's length.
 *
 * <p>The <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Arrays.html#binarySearch(java.lang.Object%5B%5D,int,int,java.lang.Object)">
 * {@code Arrays.binarySearch}</a> method is declared as
 *
 * <pre>{@code
 * class Arrays {
 *   int binarySearch(Object[] a, @IndexFor("#1") int fromIndex, @IndexOrHigh("#1") int toIndex, Object key)
 * }
 * }</pre>
 *
 * <p>Writing {@code @IndexOrHigh("arr")} is equivalent to writing {@link NonNegative @NonNegative}
 * {@link LTEqLengthOf @LTEqLengthOf("arr")}, and that is how it is treated internally by the
 * checker. Thus, if you write an {@code @IndexFor("arr")} annotation, you might see warnings about
 * {@code @NonNegative} or {@code @LTEqLengthOf}.
 *
 * @see NonNegative
 * @see LTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface IndexOrHigh {
  /** Sequences that the annotated expression is a valid index for or is equal to the lengeth of. */
  String[] value();
}
