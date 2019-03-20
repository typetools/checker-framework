package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * An annotation wrapper to allow multiple postcondition annotations.
 *
 * <p>Programmers generally do not need to use this; it is created by Java when a programmer writes
 * more than one {@code @EnsuresLTLengthOfIf} annotation at the same location.
 *
 * @see LTLengthOf
 * @see EnsuresLTLengthOf
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = LTLengthOf.class)
@InheritedAnnotation
public @interface EnsuresLTLengthOfIfMultiple {
    /**
     * Constitutes the java expression(s) that are less than the length of the given sequences after
     * the method returns the given result.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    EnsuresLTLengthOfIf[] value();
}
