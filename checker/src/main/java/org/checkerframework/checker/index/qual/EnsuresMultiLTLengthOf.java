package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * An annotation wrapper to allow multiple postcondition annotations.
 *
 * <p>Programmers generally do not need to use this; it is created by Java when a programmer writes
 * more than one {@code @EnsuresLTLengthOf} annotation at the same location.
 *
 * @see LTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = LTLengthOf.class)
@InheritedAnnotation
public @interface EnsuresLTLengthOfMultiple {
    /**
     * Constitutes The java expressions that are less than the length of the given sequences on
     * successful method termination.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    EnsuresLTLengthOf[] value();
}
