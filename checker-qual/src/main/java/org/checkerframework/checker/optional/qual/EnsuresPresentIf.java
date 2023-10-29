package org.checkerframework.checker.optional.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the given expressions of type Optional&lt;T&gt; are present, if the method returns
 * the given result (either true or false).
 *
 * <p>Here are ways this conditional postcondition annotation can be used:
 *
 * <p><b>Method parameters:</b> Suppose that a method has two arguments of type Optional&lt;T&gt;
 * and returns true if they are both equal <i>and</i> present. You could annotate the method as
 * follows:
 *
 * <pre><code> &nbsp;@EnsuresPresentIf(expression="#1", result=true)
 * &nbsp;@EnsuresPresentIf(expression="#2", result=true)
 * &nbsp;public &lt;T&gt; boolean isPresentAndEqual(Optional&lt;T&gt; optA, Optional&lt;T&gt; optB) { ... }</code>
 * </pre>
 *
 * because, if {@code isPresentAndEqual} returns true, then the first (#1) argument to {@code
 * isPresentAndEqual} was present, and so was the second (#2) argument. Note that you can write two
 * {@code @EnsuresPresentIf} annotations on a single method.
 *
 * <p><b>Fields:</b> The value expressions can refer to fields, even private ones. For example:
 *
 * <pre><code> &nbsp;@EnsuresPresentIf(expression="this.optShape", result=true)
 *  public boolean isShape() {
 *    return (this.optShape != null &amp;&amp; this.optShape.isPresent());
 *  }</code></pre>
 *
 * An {@code @EnsuresPresentIf} annotation that refers to a private field is useful for verifying
 * that a method establishes a property, even though client code cannot directly affect the field.
 *
 * <p><b>Method postconditions:</b> Suppose that if a method {@code isRectangle()} returns true,
 * then {@code getRectangle()} will return a present Optional. You an express this relationship as:
 *
 * <pre>{@code  @EnsuresPresentIf(result=true, expression="getRectangle()")
 * public @Pure isRectangle() { ... }}</pre>
 *
 * @see Present
 * @see EnsuresPresent
 * @checker_framework.manual #optional-checker Optional Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = Present.class)
@InheritedAnnotation
public @interface EnsuresPresentIf {
  /**
   * Returns the return value of the method under which the postcondition holds.
   *
   * @return the return value of the method under which the postcondition holds
   */
  boolean result();

  /**
   * Returns the Java expressions of type Optional&lt;T&gt; that are present after the method
   * returns the given result.
   *
   * @return the Java expressions of type Optional&lt;T&gt; that are present after the method
   *     returns the given result.
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * A wrapper annotation that makes the {@link EnsuresPresentIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresPresentIf} annotation at the same location.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = Present.class)
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresPresentIf[] value();
  }
}
