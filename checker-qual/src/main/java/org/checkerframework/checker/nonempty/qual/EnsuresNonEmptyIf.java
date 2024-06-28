package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the specific expressions are non-empty, if the method returns the given result
 * (either true or false).
 *
 * <p>Here are ways this conditional postcondition annotation can be used:
 *
 * <p><b>Method parameters:</b> Suppose that a method has a parameter that is a list, and returns
 * true if the length of the list is non-zero. You could annotate the method as follows:
 *
 * <pre><code>&nbsp;@EnsuresNonEmptyIf(result = true, expression = "#1")
 * &nbsp;public &lt;T&gt; boolean isLengthGreaterThanZero(List&lt;T&gt; items) { ... }</code>
 * </pre>
 *
 * because, if {@code isLengthGreaterThanZero} returns true, then {@code items} was non-empty. Note
 * that you can write more than one {@code @EnsuresNonEmptyIf} annotations on a single method.
 *
 * <p><b>Fields:</b> The value expression can refer to fields, even private ones. For example:
 *
 * <pre><code>&nbsp;@EnsuresNonEmptyIf(result = true, expression = "this.orders")
 * &nbsp;public &lt;T&gt; boolean areOrdersActive() {
 *    return this.orders != null &amp;&amp; this.orders.size() &gt; 0;
 * }</code></pre>
 *
 * An {@code @EnsuresNonEmptyIf} annotation that refers to a private field is useful for verifying
 * that a method establishes a property, even though client code cannot directly affect the field.
 *
 * <p><b>Method postconditions:</b> Suppose that if a method {@code areOrdersActive()} returns true,
 * then {@code getOrders()} will return a non-empty Map. You can express this relationship as:
 *
 * <pre><code>&nbsp;@EnsuresNonEmptyIf(result = true, expression = "this.getOrders()")
 * &nbsp;public &lt;T&gt; boolean areOrdersActive() {
 *    return this.orders != null &amp;&amp; this.orders.size() &gt; 0;
 * }</code></pre>
 *
 * @see NonEmpty
 * @see EnsuresNonEmpty
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = NonEmpty.class)
@InheritedAnnotation
public @interface EnsuresNonEmptyIf {

  /**
   * A return value of the method; when the method returns that value, the postcondition holds.
   *
   * @return the return value of the method for which the postcondition holds
   */
  boolean result();

  /**
   * Returns the Java expressions that are non-empty after the method returns the given result.
   *
   * @return the Java expressions that are non-empty after the method returns the given result
   */
  String[] expression();

  /**
   * A wrapper annotation that makes the {@link EnsuresNonEmptyIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write ths. It is created by Java when a programmer
   * writes more than one {@link EnsuresNonEmptyIf} annotation at the same location.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = NonEmpty.class)
  @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresNonEmptyIf[] value();
  }
}
