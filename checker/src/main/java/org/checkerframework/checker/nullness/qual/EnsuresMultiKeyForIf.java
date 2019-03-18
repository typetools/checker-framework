package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * An annotation wrapper to allow multiple {@code @EnsuresKeyForIf} annotation at the same location.
 *
 * <p>Programmers generally do not need to use this; it is created by Java when a programmer writes
 * more than one {@code @EnsuresEnsuresKeyForIf} annotation at the same location.
 *
 * @see EnsuresKeyForIf
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = KeyFor.class)
@InheritedAnnotation
public @interface EnsuresMultiKeyForIf {
    /**
     * Constitutes the Java expressions that are keys in the given maps after the method returns the given result.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    EnsuresKeyForIf[] value();
}
