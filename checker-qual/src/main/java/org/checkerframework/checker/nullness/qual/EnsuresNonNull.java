package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

// TODO: In a fix for https://tinyurl.com/cfissue/1917, add the text:  Every prefix expression is
// also non-null; for example, {@code @EnsuresNonNull(expression="a.b.c")} implies that both {@code
// a.b} and {@code a.b.c} are non-null.
/**
 * Indicates that the value expressions are non-null just after a method call, if the method
 * terminates successfully.
 *
 * <p>This postcondition annotation is useful for methods that initialize a field:
 *
 * <pre><code>
 *  {@literal @}EnsuresNonNull("theMap")
 *  void initialize() {
 *    theMap = new HashMap&lt;&gt;();
 *  }
 * </code></pre>
 *
 * It can also be used for a method that fails if a given expression is null, indicating that the
 * argument is null if the method returns normally:
 *
 * <pre><code>
 *  /** Throws an exception if the argument is null. *&#47;
 *  {@literal @}EnsuresNonNull("#1")
 *  void assertNonNull(Object arg) { ... }
 * </code></pre>
 *
 * @see EnsuresNonNullIf
 * @see NonNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
@Repeatable(EnsuresNonNull.List.class)
public @interface EnsuresNonNull {
  /**
   * Returns Java expressions that are {@link NonNull} after successful method termination.
   *
   * @return Java expressions that are {@link NonNull} after successful method termination
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] value();

  /**
   * A wrapper annotation that makes the {@link EnsuresNonNull} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresNonNull} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @PostconditionAnnotation(qualifier = NonNull.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresNonNull[] value();
  }
}
