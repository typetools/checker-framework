package org.checkerframework.checker.calledmethods.qual;

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
 * Indicates that the method, if it terminates with the given result, invokes the given methods on
 * the given expressions.
 *
 * @see EnsuresCalledMethods
 * @see CalledMethods
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = CalledMethods.class)
@InheritedAnnotation
@Repeatable(EnsuresCalledMethodsIf.List.class)
public @interface EnsuresCalledMethodsIf {
  /**
   * Returns the return value of the method under which the postcondition holds.
   *
   * @return the return value of the method under which the postcondition holds
   */
  boolean result();

  /**
   * Returns Java expressions that have had the given methods called on them after the method
   * returns {@link #result}.
   *
   * @return an array of Java expressions
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * The methods guaranteed to be invoked on the expressions if the result of the method is {@link
   * #result}.
   *
   * @return the methods guaranteed to be invoked on the expressions if the result of the method is
   *     {@link #result}
   */
  @QualifierArgument("value")
  String[] methods();

  /**
   * A wrapper annotation that makes the {@link EnsuresCalledMethodsIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresCalledMethodsIf} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = CalledMethods.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresCalledMethodsIf[] value();
  }
}
