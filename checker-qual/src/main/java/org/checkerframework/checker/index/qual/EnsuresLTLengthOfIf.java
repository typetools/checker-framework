package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the given expressions evaluate to an integer whose value is less than the lengths
 * of all the given sequences, if the method returns the given result (either true or false).
 *
 * <p>As an example, consider the following method:
 *
 * <pre>
 *      &#64;EnsuresLTLengthOfIf(
 *          expression = "end",
 *          result = true,
 *          targetValue = "array",
 *          offset = "#1 - 1"
 *      )
 *      public boolean tryShiftIndex(&#64;NonNegative int x) {
 *          int newEnd = end - x;
 *          if (newEnd &#60; 0) {
 *             return false;
 *          }
 *          end = newEnd;
 *          return true;
 *      }
 * </pre>
 *
 * Calling this function ensures that the field {@code end} of the {@code this} object is of type
 * {@code @LTLengthOf(value = "array", offset = "x - 1")}, for the value {@code x} that is passed as
 * the argument. This allows the Index Checker to verify that {@code end + x} is an index into
 * {@code array} in the following code:
 *
 * <pre>
 *      public void useTryShiftIndex(&#64;NonNegative int x) {
 *          if (tryShiftIndex(x)) {
 *              Arrays.fill(array, end, end + x, null);
 *          }
 *      }
 * </pre>
 *
 * @see LTLengthOf
 * @see EnsuresLTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = LTLengthOf.class)
@InheritedAnnotation
@Repeatable(EnsuresLTLengthOfIf.List.class)
public @interface EnsuresLTLengthOfIf {
  /**
   * The return value of the method that needs to hold for the postcondition to hold.
   *
   * @return the return value of the method that needs to hold for the postcondition to hold
   */
  boolean result();

  /**
   * Java expression(s) that are less than the length of the given sequences after the method
   * returns the given result.
   *
   * @return Java expression(s) that are less than the length of the given sequences after the
   *     method returns the given result
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * Sequences, each of which is longer than each of the expressions' value after the method returns
   * the given result.
   */
  @JavaExpression
  @QualifierArgument("value")
  String[] targetValue();

  /**
   * This expression plus each of the expressions is less than the length of the sequence after the
   * method returns the given result. The {@code offset} element must ether be empty or the same
   * length as {@code targetValue}.
   *
   * @return the offset expressions
   */
  @JavaExpression
  @QualifierArgument("offset")
  String[] offset() default {};

  /**
   * A wrapper annotation that makes the {@link EnsuresLTLengthOfIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresLTLengthOfIf} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = LTLengthOf.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresLTLengthOfIf[] value();
  }
}
