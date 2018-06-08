package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * A sequence whose declaration is annotated with this annotation contains a named subsequence. This
 * annotation permits indices for the subsequence to be translated into indices for this sequence
 * and vice-versa.
 *
 * <p>Consider the following example (note that because this annotation is a declaration annotation,
 * it appears immediately before the declaration of the field - not as the primary type on the
 * array!):
 *
 * <pre><code>
 *   class IntSubArray {
 *   {@literal @}HasSubsequence(value = "this", from = "this.start", to = "this.end") int [] array;
 *    int {@literal @}IndexFor("array") int start;
 *    int {@literal @}IndexOrHigh("array") int end;
 *   }
 * </code></pre>
 *
 * These annotations allow two kinds of indexing operations that would otherwise be forbidden:
 *
 * <ul>
 *   <li>If {@code i} is {@code @IndexFor("this")}, then {@code start + i} is
 *       {@code @IndexFor("array")}.
 *   <li>If {@code j} is {@code @IndexFor("array")}, then {@code j - start } is
 *       {@code @IndexFor("this")}.
 * </ul>
 *
 * When assigning an array {@code a} to {@code array}, 3 facts need to be proven:
 *
 * <ul>
 *   <li>{@code start} is {@code @NonNegative}.
 *   <li>{@code end} is {@code @LTEqLengthOf("a")}.
 *   <li>{@code start} is {@code @LessThan("end + 1")}.
 * </ul>
 *
 * A warning will still be issued at this assignment, because the Index Checker cannot prove that
 * {@code "this"} is the name of the subsequence, only that such a subsequence exists. You should
 * manually verify that the named sequence is, in fact, the subsequence described in the annotation
 * and then suppress the warning.
 *
 * <p>For an example of how this annotation is used in practice, see the test GuavaPrimitives.java
 * in /checker/tests/index/.
 *
 * <p>This annotation may only be written on fields.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Target({ElementType.FIELD})
public @interface HasSubsequence {
    /* The name of the subsequence. */
    @JavaExpression
    String value();

    /* The first valid index into the subsequence. */
    @JavaExpression
    String from();

    /* The end of the subsequence. This value is *not* an index into the subsequence. */
    @JavaExpression
    String to();
}
