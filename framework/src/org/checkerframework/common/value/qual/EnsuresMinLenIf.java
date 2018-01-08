package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the value of the given expression is a sequence containing at least the given
 * number of elements, if the method returns the given result (either true or false).
 *
 * <p>When the annotated method returns {@code result}, then all the expressions in {@code
 * expression} are considered to be {@code MinLen(targetValue)}.
 *
 * @see MinLen
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@ConditionalPostconditionAnnotation(qualifier = MinLen.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InheritedAnnotation
public @interface EnsuresMinLenIf {
    /**
     * Java expression(s) that are a sequence with the given minimum length after the method returns
     * the given result.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /** The return value of the method that needs to hold for the postcondition to hold. */
    boolean result();

    /** The minimum number of elements in the sequence. */
    @QualifierArgument("value")
    int targetValue() default 0;
}
