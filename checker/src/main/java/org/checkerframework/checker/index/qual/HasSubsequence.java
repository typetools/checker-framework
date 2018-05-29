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
 *    {@literal @}HasSubsequence(value = "this", from = "this.start", to = "this.end") int [] array;
 *    int {@literal @}IndexFor("array") int start;
 *    int {@literal @}IndexOrHigh("array") int end;
 *   }
 * </code></pre>
 *
 * These annotations allow two kinds of indexing operations that would otherwise be forbidden:
 *
 * <ul>
 *   <li>If <code>i</code> is <code>{@literal @}IndexFor("this")</code>, then <code>start + i</code>
 *       is <code>{@literal @}IndexFor("array")</code>.
 *   <li>If <code>j</code> is <code>{@literal @}IndexFor("array")</code>, then <code>j - start
 *       </code> is <code>{@literal @}IndexFor("this")</code>.
 * </ul>
 *
 * When assigning an array <code>a</code> to <code>array</code>, 3 facts need to be proven:
 *
 * <ul>
 *   <li><code>start</code> is <code>{literal @}NonNegative</code>.
 *   <li><code>end</code> is <code>{literal @}LTEqLengthOf("a")</code>.
 *   <li><code>start</code> is <code>{literal @}LessThan("end + 1")</code>.
 * </ul>
 *
 * A warning will still be issued at this assignment, because the Index Checker cannot prove that
 * <code>"this"</code> is the name of the subsequence, only that such a subsequence exists. You
 * should manually verify that the named sequence is, in fact, the subsequence described in the
 * annotation and then suppress the warning.
 *
 * <p>For an example of how this annotation is used in practice, see the test GuavaPrimitives.java
 * in /checker/tests/index/.
 *
 * <p>Though this annotation technically permits its user to write multiple values in each of its
 * fields, writing more than one String to any of them is an error, and the behavior of the Index
 * Checker is undefined.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface HasSubsequence {
    /* The name of the subsequence. */
    @JavaExpression
    String[] value();

    /* The first valid index into the subsequence. */
    @JavaExpression
    String[] from();

    /* The end of the subsequence. This value is *not* an index into the subsequence. */
    @JavaExpression
    String[] to();
}
