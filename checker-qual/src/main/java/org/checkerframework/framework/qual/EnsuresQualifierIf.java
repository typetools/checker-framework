package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A conditional postcondition annotation to indicate that a method ensures that certain expressions
 * have a certain qualifier once the method has terminated, and if the result is as indicated by
 * {@code result}. The expressions for which the qualifier holds after the method's execution are
 * indicated by {@code expression} and are specified using a string. The qualifier is specified by
 * the {@code qualifier} annotation element.
 *
 * <p>Here is an example use:
 *
 * <pre><code>
 *   {@literal @}EnsuresQualifierIf(result = true, expression = "#1", qualifier = Odd.class)
 *    boolean isOdd(int p1, int p2) {
 *        return p1 % 2 == 1;
 *    }
 * </code></pre>
 *
 * <p>This annotation is only applicable to methods with a boolean return type.
 *
 * <p>Some type systems have specialized versions of this annotation, such as {@code
 * org.checkerframework.checker.nullness.qual.EnsuresNonNullIf} and {@code
 * org.checkerframework.checker.lock.qual.EnsuresLockHeldIf}.
 *
 * @see EnsuresQualifier
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@InheritedAnnotation
@Repeatable(EnsuresQualifierIf.List.class)
public @interface EnsuresQualifierIf {
  /**
   * Returns the return value of the method that needs to hold for the postcondition to hold.
   *
   * @return the return value of the method that needs to hold for the postcondition to hold
   */
  boolean result();

  /**
   * Returns the Java expressions for which the qualifier holds if the method terminates with return
   * value {@link #result()}.
   *
   * @return the Java expressions for which the qualifier holds if the method terminates with return
   *     value {@link #result()}
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * Returns the qualifier that is guaranteed to hold if the method terminates with return value
   * {@link #result()}.
   *
   * @return the qualifier that is guaranteed to hold if the method terminates with return value
   *     {@link #result()}
   */
  Class<? extends Annotation> qualifier();

  /**
   * A wrapper annotation that makes the {@link EnsuresQualifierIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresQualifierIf} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @InheritedAnnotation
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresQualifierIf[] value();
  }
}
