package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * Indicates that the value expressions are non-null, if the method terminates successfully.
 *
 * <p>This postcondition annotation is useful for methods that initialize a field:
 *
 * <pre><code>
 * {@literal @}EnsuresNonNull("theMap")
 *  public static void initialize() {
 *    theMap = new HashMap&lt;&gt;();
 *  }
 * </code></pre>
 *
 * It can also be used for a method that fails if a given expression is null:
 *
 * <pre><code>
 *  /** Throws an exception if the argument is null. *&#47;
 * {@literal @}EnsuresNonNull("#1")
 *  void assertNonNull(Object arg) { ... }
 * </code></pre>
 *
 * @see NonNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
public @interface EnsuresNonNull {
    /**
     * The Java expressions that are ensured to be {@link NonNull} on successful method termination.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] value();
}
