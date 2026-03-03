package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A postcondition annotation to indicate that a method ensures that certain expressions have a
 * certain type qualifier once the method has successfully terminated. The expressions for which the
 * qualifier holds after the method's execution are indicated by {@code expression} and are
 * specified using a string. The qualifier is specified by the {@code qualifier} annotation element.
 *
 * <p>Here is an example use:
 *
 * <pre><code>
 *  {@literal @}EnsuresQualifier(expression = "p.f1", qualifier = Odd.class)
 *   void oddF1_1() {
 *       p.f1 = null;
 *   }
 * </code></pre>
 *
 * Some type systems have specialized versions of this annotation, such as {@code
 * org.checkerframework.checker.nullness.qual.EnsuresNonNull} and {@code
 * org.checkerframework.checker.lock.qual.EnsuresLockHeld}.
 *
 * @see EnsuresQualifierIf
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InheritedAnnotation
@Repeatable(EnsuresQualifier.List.class)
public @interface EnsuresQualifier {
  /**
   * Returns the Java expressions for which the qualifier holds after successful method termination.
   *
   * @return the Java expressions for which the qualifier holds after successful method termination
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * Returns the qualifier that is guaranteed to hold on successful termination of the method.
   *
   * @return the qualifier that is guaranteed to hold on successful termination of the method
   */
  Class<? extends Annotation> qualifier();

  /**
   * A wrapper annotation that makes the {@link EnsuresQualifier} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresQualifier} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresQualifier[] value();
  }
}
