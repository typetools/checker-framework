package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * Indicates that a particular expression evaluates to a non-empty value, if the method terminates
 * successfully.
 *
 * <p>This annotation applies to {@link java.util.Collection}, {@link java.util.Iterator}, {@link
 * java.lang.Iterable}, and {@link java.util.Map}, but not {@link java.util.Optional}.
 *
 * <p>This postcondition annotation is useful for methods that make a value non-empty by side
 * effect:
 *
 * <pre><code>
 *   {@literal @}EnsuresNonEmpty("ids")
 *   void addId(String id) {
 *     ids.add(id);
 *   }
 * </code></pre>
 *
 * It can also be used for a method that fails if a given value is empty, indicating that the
 * argument is non-empty if the method returns normally:
 *
 * <pre><code>
 *   /** Throws an exception if the argument is empty. *&#47;
 *   {@literal @}EnsuresNonEmpty("#1")
 *   void useTheMap(Map&lt;T, U&gt; arg) { ... }
 * </code></pre>
 *
 * @see NonEmpty
 * @see org.checkerframework.checker.nonempty.NonEmptyChecker
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = NonEmpty.class)
@InheritedAnnotation
public @interface EnsuresNonEmpty {
  /**
   * The expression (a collection, iterator, iterable, or map) that is non-empty, if the method
   * returns normally.
   *
   * @return the expression (a collection, iterator, iterable, or map) that is non-empty, if the
   *     method returns normally
   */
  String[] value();
}
