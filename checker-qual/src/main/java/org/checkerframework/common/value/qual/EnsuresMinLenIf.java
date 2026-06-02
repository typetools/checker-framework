package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the value of the given expression is a sequence containing at least the given
 * number of elements, if the method returns the given result (either true or false).
 *
 * <p>When the annotated method returns {@code result}, then all the expressions in {@code
 * expression} are considered to be {@code MinLen(targetValue)}.
 *
 * @see MinLen
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = MinLen.class)
@InheritedAnnotation
@Repeatable(EnsuresMinLenIf.List.class)
public @interface EnsuresMinLenIf {
  /**
   * Returns the return value of the method under which the postcondition to hold.
   *
   * @return the return value of the method under which the postcondition to hold
   */
  boolean result();

  /**
   * Returns Java expression(s) that are a sequence with the given minimum length after the method
   * returns {@link #result}.
   *
   * @return an array of Java expression(s), each of which is a sequence with the given minimum
   *     length after the method returns {@link #result}
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * Returns the minimum number of elements in the sequence.
   *
   * @return the minimum number of elements in the sequence
   */
  @QualifierArgument("value")
  int targetValue() default 0;

  /**
   * A wrapper annotation that makes the {@link EnsuresMinLenIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresMinLenIf} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = MinLen.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresMinLenIf[] value();
  }
}
