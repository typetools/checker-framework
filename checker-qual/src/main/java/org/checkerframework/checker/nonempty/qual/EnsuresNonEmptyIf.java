package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the given expressions are non-empty if the method returns the given result (either
 * true or false).
 *
 * <p>As an example, consider the following method in {@code java.util.Map}:
 *
 * <pre>
 *   &#64;EnsuresNonEmptyIf(result=true, expression="this")
 *   public boolean containsKey(String key) { ... }
 * </pre>
 *
 * If an invocation {@code m.containsKey(k)} returns true, then the type of {@code this} can be
 * inferred to be {@code @NonEmpty}.
 *
 * @see NonEmpty
 * @see EnsuresNonEmpty
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = NonEmpty.class)
@InheritedAnnotation
public @interface EnsuresNonEmptyIf {
    /**
     * The value the method must return, in order for the postcondition to hold.
     *
     * @return The value the method must return, in order for the postcondition to hold
     */
    boolean result();

    /**
     * Java expressions that are non-empty after the method returns the given result.
     *
     * @return Java expressions that are non-empty after the method returns the given result
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();
}
