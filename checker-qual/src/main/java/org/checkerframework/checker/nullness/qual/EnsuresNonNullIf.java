package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

// TODO: In a fix for https://tinyurl.com/cfissue/1917, add the text:  Every prefix expression is
// also non-null; for example, {@code @EnsuresNonNullIf(expression="a.b.c", results=true)} implies
// that both {@code a.b} and {@code a.b.c} are non-null if the method returns {@code true}.
/**
 * Indicates that the given expressions are non-null, if the method returns the given result (either
 * true or false).
 *
 * <p>Here are ways this conditional postcondition annotation can be used:
 *
 * <p><b>Method parameters:</b> A common example is that the {@code equals} method is annotated as
 * follows:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="#1", result=true)
 *   public boolean equals(@Nullable Object obj) { ... }}</pre>
 *
 * because, if {@code equals} returns true, then the first (#1) argument to {@code equals} was not
 * null.
 *
 * <p><b>Fields:</b> The value expressions can refer to fields, even private ones. For example:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="this.derived", result=true)
 *   public boolean isDerived() {
 *     return (this.derived != null);
 *   }}</pre>
 *
 * As another example, an {@code Iterator} may cache the next value that will be returned, in which
 * case its {@code hasNext} method could be annotated as:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="next_cache", result=true)
 *   public boolean hasNext() {
 *     if (next_cache == null) {
 *       return false;
 *     }
 *     ...
 *   }}</pre>
 *
 * An {@code EnsuresNonNullIf} annotation that refers to a private field is useful for verifying
 * that a method establishes a property, even though client code cannot directly affect the field.
 *
 * <p><b>Method calls:</b> If {@link Class#isArray()} returns true, then {@link
 * Class#getComponentType()} returns non-null. You can express this relationship as:
 *
 * <pre>{@code   @EnsuresNonNullIf(expression="getComponentType()", result=true)
 *   public native @Pure boolean isArray();}</pre>
 *
 * You can write two {@code @EnsuresNonNullIf} annotations on a single method:
 *
 * <pre><code>
 * &nbsp;   @EnsuresNonNullIf(expression="outputFile", result=true)
 * &nbsp;   @EnsuresNonNullIf(expression="memoryOutputStream", result=false)
 *     public boolean isThresholdExceeded() { ... }
 * </code></pre>
 *
 * @see NonNull
 * @see EnsuresNonNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
@Repeatable(EnsuresNonNullIf.List.class)
public @interface EnsuresNonNullIf {
  /**
   * Returns the return value of the method under which the postcondition holds.
   *
   * @return the return value of the method under which the postcondition holds
   */
  boolean result();

  /**
   * Returns Java expression(s) that are non-null after the method returns the given result.
   *
   * @return Java expression(s) that are non-null after the method returns the given result
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] expression();

  /**
   * * A wrapper annotation that makes the {@link EnsuresNonNullIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresNonNullIf} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = NonNull.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresNonNullIf[] value();
  }
}
