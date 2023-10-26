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
 * <p><b>Method parameters: </b>A method that has two arguments of type Optional&lt;T&gt; and
 * returns true if they are both equal <i>and</i> present might be annotated as follows:
 *
 * <pre>{@code @EnsuresPresentIf(expression="#1", result=true)
 * public <T> boolean isPresentAndEqual(Optional<T> optA, Optional<T> optB) { ... }}</pre>
 *
 * because, if {@code isPresentAndEqual} returns true, then the first (#1) argument to {@code
 * isPresentAndEqual} was present.
 *
 * <p><b>Fields:</b> The value expressions can refer to fields, even private ones. For example:
 *
 * <pre>{@code @EnsuresPresentIf(expression="this.optShape", result=true)
 * public boolean isShape() {
 *   return (this.optShape != null && this.optShape.isPresent());
 * }}</pre>
 *
 * An {@code EnsuresPresentIf} annotation that refers to a private field is useful for verifying
 * that client code performs needed checks in the right order, even if the client code cannot
 * directly affect the field.
 *
 * <p><b>Method calls:</b> If a method {@code isRectangle()} returns true, then {@code
 * getRectangle()} will return a present Optional. You an express this relationship as:
 *
 * <pre>{@code @EnsuresPresentIf(expression="getRectangle()", result=true)
 * public @Pure isRectangle() { ... }}</pre>
 *
 * You can also write two {@code @EnsurePresentIf} annotations on a single method:
 *
 * <pre><code>
 * &nbsp;   @EnsuresNonNullIf(expression="#1", result=true)
 * &nbsp;   @EnsuresNonNullIf(expression="#2", result=false)
 *     public &lt;T&gt; boolean isPresentAndEqual(Optional&lt;T&gt; optA, Optional&lt;T&gt; optB) { ... }
 * </code></pre>
 *
 * @see Present
 * @see EnsuresPresent
 * @see org.checkerframework.checker.optional.OptionalChecker
 * @checker_framework.manual #optional-checker Optional Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = Present.class)
@InheritedAnnotation
public @interface EnsuresPresentIf {
  /**
   * Returns the Java expressions of type Optional&lt;T&gt; that are present after the method
   * returns the given result.
   *
   * @return the Java expressions of type Optional&lt;T&gt; that are present after the method
   *     returns the given result. value {@link #result()}
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * Returns the return value of the method under which the postcondition holds.
   *
   * @return the return value of the method under which the postcondition holds.
   */
  boolean result();

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
     * @return the repeatable annotations.
     */
    EnsuresPresentIf[] value();
  }
}
