package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The annotated expression evaluates to either -1 or a non-negative integer less than the lengths
 * of all the given sequences. The annotation {@code @SubstringIndexFor(args)} is like <code>
 * {@literal @}{@link NonNegative} {@literal @}{@link LTLengthOf}(args)</code>, except that
 * {@code @SubstringIndexFor(args)} additionally permits the value -1.
 *
 * <p>When multiple values or offsets are given, they are considered pairwise. For example,
 * {@code @SubstringIndexFor(value={"a", "b"}, offset={"c", "d"})} is equivalent to writing both
 * {@code @SubstringIndexFor(value="a", offset="c")} and {@code @SubstringIndexFor(value="b",
 * offset="d")}.
 *
 * <p>The return types of JDK methods {@link java.lang.String#indexOf(String) String.indexOf} and
 * {@link java.lang.String#lastIndexOf(String) String.lastIndexOf} are annotated
 * {@code @SubstringIndexFor(value="this",offset="#1.length()-1")}. This means that the returned
 * value is either -1 or it is less than or equal to the length of the receiver sequence minus the
 * length of the sequence passed as the first argument.
 *
 * <p>The name of this annotation, "substring index for", is intended to mean that the annotated
 * expression is a index of a substring (returned by {@code indexOf} or similar methods) for the
 * specified sequence.
 *
 * @checker_framework.manual #index-substringindex Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SubstringIndexUnknown.class)
public @interface SubstringIndexFor {
    /**
     * Sequences, each of which is longer than the annotated expression plus the corresponding
     * {@code offset}. (Exception: the annotated expression is also allowed to have the value -1.)
     */
    @JavaExpression
    public String[] value();

    /**
     * This expression plus the annotated expression is less than the length of the corresponding
     * sequence in the {@code value} array. (Exception: the annotated expression is also allowed to
     * have the value -1.)
     *
     * <p>The {@code offset} array must either be empty or the same length as {@code value}.
     *
     * <p>The expressions in {@code offset} may be addition/subtraction of any number of Java
     * expressions. For example, {@code @SubstringIndexFor(value = "a", offset = "b.length() - 1")}.
     */
    @JavaExpression
    public String[] offset();
}
