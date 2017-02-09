package org.checkerframework.checker.index.qual;

/**
 * An integer that is either -1 or is a valid index for each of the given sequences.
 *
 * <p>The <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#indexOf-java.lang.String-">
 * <code>String.indexOf(String)</code></a> method is declared as
 *
 * <pre><code>
 *   class String {
 *    {@literal @}IndexOrLow("this") int indexOf(String str) { ... }
 *   }
 * </code></pre>
 *
 * <p>Writing {@code @IndexOrLow("arr")} is equivalent to writing {@link
 * GTENegativeOne @GTENegativeOne} {@link LTLengthOf @LTLengthOf("arr")}, and that is how it is
 * treated internally by the checker. Thus, if you write an {@code @IndexOrLow("arr")} annotation,
 * you might see warnings about {@code @GTENegativeOne} or {@code @LTLengthOf}.
 *
 * @see GTENegativeOne
 * @see LTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
public @interface IndexOrLow {
    /** Sequences that the annotated expression is a valid index for (or it's -1). */
    String[] value() default {};
}
