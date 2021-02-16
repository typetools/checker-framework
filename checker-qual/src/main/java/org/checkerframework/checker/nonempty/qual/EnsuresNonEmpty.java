package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * Indicates that the value expressions are non-empty if the method terminates successfully.
 *
 * <p>Consider the following method from {@code java.util.Map}:
 *
 * <pre>
 * &#64;EnsuresNonEmpty(expression="this")
 * public @Nullable V put(K key, V value) { ... }
 * </pre>
 *
 * <p>This method guarantees that {@code this} has type {@code @NonEmpty} after the method returns.
 *
 * @see NonEmpty
 * @see EnsuresNonEmptyIf
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = NonEmpty.class)
@InheritedAnnotation
public @interface EnsuresNonEmpty {
    /**
     * Java expressions that are non-empty on successful method termination.
     *
     * @return Java expressions that are non-empty on successful method termination
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    @JavaExpression
    String[] value();
}
