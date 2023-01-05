package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * The annotated sequence contains a subsequence that is equal to the value of some other
 * expression. This annotation permits the Upper Bound Checker to translate indices for one sequence
 * into indices for the other sequence.
 *
 * <p>Consider the following example:
 *
 * <pre><code>
 *  class IntSubArray {
 *    {@literal @}HasSubsequence(subsequence = "this", from = "this.start", to = "this.end")
 *    int [] array;
 *    {@literal @}IndexFor("array") int start;
 *    {@literal @}IndexOrHigh("array") int end;
 *  }
 * </code></pre>
 *
 * The above annotations mean that the value of an {@code IntSubArray} object is equal to a
 * subsequence of its {@code array} field.
 *
 * <p>These annotations imply the following relationships among {@code @}{@link IndexFor}
 * annotations:
 *
 * <ul>
 *   <li>If {@code i} is {@code @IndexFor("this")}, then {@code this.start + i} is
 *       {@code @IndexFor("array")}.
 *   <li>If {@code j} is {@code @IndexFor("array")}, then {@code j - this.start } is
 *       {@code @IndexFor("this")}.
 * </ul>
 *
 * When assigning an array {@code a} to {@code array}, 4 facts need to be true:
 *
 * <ul>
 *   <li>{@code start} is {@code @NonNegative}.
 *   <li>{@code end} is {@code @LTEqLengthOf("a")}.
 *   <li>{@code start} is {@code @LessThan("end + 1")}.
 *   <li>the value of {@code this} equals {@code array[start..end-1]}
 * </ul>
 *
 * The Index Checker verifies the first 3 facts, but always issues a warning because it cannot prove
 * the 4th fact. The programmer should manually verify that the {@code subsequence} field is equal
 * to the given subsequence and then suppress the warning.
 *
 * <p>For an example of how this annotation is used in practice, see the test GuavaPrimitives.java
 * in /checker/tests/index/.
 *
 * <p>This annotation may only be written on fields.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface HasSubsequence {
  /** An expression that evaluates to the subsequence. */
  @JavaExpression
  String subsequence();

  /** The index into this where the subsequence starts. */
  @JavaExpression
  String from();

  /** The index into this, immediately past where the subsequence ends. */
  @JavaExpression
  String to();
}
