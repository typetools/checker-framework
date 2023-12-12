package org.checkerframework.checker.optional.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * Indicates that the expression evaluates to a non-empty Optional, if the method terminates
 * successfully.
 *
 * <p>This postcondition annotation is useful for methods that construct a non-empty Optional:
 *
 * <pre><code>
 *   {@literal @}EnsuresPresent("optStr")
 *   void initialize() {
 *     optStr = Optional.of("abc");
 *   }
 * </code></pre>
 *
 * It can also be used for a method that fails if a given Optional value is empty, indicating that
 * the argument is present if the method returns normally:
 *
 * <pre><code>
 *   /** Throws an exception if the argument is empty. *&#47;
 *   {@literal @}EnsuresPresent("#1")
 *   void useTheOptional(Optional&lt;T&gt; arg) { ... }
 * </code></pre>
 *
 * @see Present
 * @see org.checkerframework.checker.optional.OptionalChecker
 * @checker_framework.manual #optional-checker Optional Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = Present.class)
@InheritedAnnotation
public @interface EnsuresPresent {
  /**
   * The expression (of Optional type) that is present, if the method returns normally.
   *
   * @return the expression (of Optional type) that is present, if the method returns normally
   */
  String[] value();
}
