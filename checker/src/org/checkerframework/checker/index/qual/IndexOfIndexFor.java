package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to either -1 or a less than the lengths of all the given
 * sequences. The restrictions that this annotation poses on the set of possible values of the
 * annotated expression are similar to the {@link LTLengthOf} annotation with the same parameters,
 * except that this annotation additionally allows the value to be -1.
 *
 * <p>This is the return type of {@link java.lang.String#indexOf(String) String.indexOf} and {@link
 * java.lang.String#lastIndexOf(String) String.lastIndexOf} in the JDK. The return type of these
 * methods is annotated {@code @IndexOfIndexFor(value="this",offset="#1.length()-1")}, meaning that
 * the returned value is either -1 or it is less than or equal to the length of the receiver
 * sequence minus the length of the sequence passed as the first argument.
 *
 * <p>The name of this annotation, "indexOf index for", is to mean that the annotated expression is
 * a index returned by {@code indexOf} for the specified sequence.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(IndexOfUnknown.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface IndexOfIndexFor {
    /**
     * Sequences, each of which is longer than the annotated expression's value with the
     * corresponding offset added, except when the annotated expression's value is -1.
     */
    @JavaExpression
    public String[] value();

    /**
     * Array of offset expressions. For each offset expression, if the annotated expression's value
     * is not -1, then the the annotated expression's value plus the offset expression's value is
     * less than the length of the corresponding sequence in the {@code value} array. The {@code
     * offset} array must either be empty or the same length as {@code value}.
     *
     * <p>The expressions in {@code offset} may be addition/subtraction of any number of Java
     * expressions. For example, {@code @IndexOfIndexFor(value = "a", offset = "b.length() - 1")}.
     */
    @JavaExpression
    public String[] offset();
}
