package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the value expressions evaluate to an integer whose value is less than the lengths
 * of all the given sequences, if the method terminates successfully.
 *
 * <p>Consider the following example, from the Index Checker's regression tests:
 *
 * <pre>
 *
 * {@code @EnsuresLTLengthOf(value = "end", targetValue = "array", offset = "#1 - 1")
 *  public void shiftIndex(@NonNegative int x) {
 *      int newEnd = end - x;
 *      if (newEnd < 0) throw new RuntimeException();
 *      end = newEnd;
 *  }
 * }
 * </pre>
 *
 * where {@code end} is annotated as {@code @NonNegative @LTEqLengthOf("array") int end;}
 *
 * <p>What this method guarantees is that end has a particular type - @LTLengthOf(value="array",
 * offset="x - 1") - after the method returns. This is useful in cases like this one:
 *
 * <pre>{@code
 * public void useShiftIndex(@NonNegative int x) {
 *    // :: error: (argument.type.incompatible)
 *    Arrays.fill(array, end, end + x, null);
 *    shiftIndex(x);
 *    Arrays.fill(array, end, end + x, null);
 * }
 * }</pre>
 *
 * The first call to {@code Arrays.fill} is rejected (hence the comment about an error). But, after
 * calling {@code shiftIndex(x)}, {@code end} has an annotation that allows the {@code end + x} to
 * be accepted as {@code @LTLengthOf("array")}.
 *
 * @see LTLengthOf
 * @see org.checkerframework.checker.index.IndexChecker
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = LTLengthOf.class)
@InheritedAnnotation
public @interface EnsuresLTLengthOf {
    /**
     * The Java expressions that are less than the length of the given sequences on successful
     * method termination.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] value();

    /**
     * Sequences, each of which is longer than the each of the expressions' value on successful
     * method termination.
     */
    @JavaExpression
    @QualifierArgument("value")
    String[] targetValue();

    /**
     * This expression plus each of the value expressions is less than the length of the sequence on
     * successful method termination. The {@code offset} element must ether be empty or the same
     * length as {@code targetValue}.
     */
    @JavaExpression
    @QualifierArgument("offset")
    String[] offset() default {};
}
