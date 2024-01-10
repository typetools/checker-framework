package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * Indicates that the expression evaluates to a non-empty {@link java.util.Collection collection},
 * {@link java.util.Iterator iterator}, {@link java.lang.Iterable iterable}, or {@link java.util.Map
 * map}, if the method terminates successfully.
 *
 * <p>This postcondition annotation is useful for methods that construct a non-empty collection,
 * iterator, iterable, or map:
 *
 * <pre><code>
 *   {@literal @}EnsuresNonEmpty("ids")
 *   void addId(String id) {
 *     ids.add(id);
 *   }
 * </code></pre>
 *
 * It can also be used for a method that fails if a given collection, iterator, iterable, or map is
 * empty, indicating that the argument is non-empty if the method returns normally:
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
