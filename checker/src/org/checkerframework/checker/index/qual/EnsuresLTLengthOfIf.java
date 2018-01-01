package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the given expressions evaluate to an integer whose value is less than the lengths
 * of all the given sequences, if the method returns the given result (either true or false).
 *
 * @see LTLengthOf
 * @see EnsuresLTLengthOf
 * @see org.checkerframework.checker.index.IndexChecker
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(
    qualifier = LTLengthOf.class,
    sourceArguments = {"targetValue", "offset"},
    targetArguments = {"value, offset"}
)
@InheritedAnnotation
public @interface EnsuresLTLengthOfIf {
    /**
     * Java expression(s) that are less than the length of the given sequences after the method
     * returns the given result.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] expression();

    /** The return value of the method that needs to hold for the postcondition to hold. */
    boolean result();

    /**
     * Sequences, each of which is longer than each of the expressions' value after the method
     * returns the given result.
     */
    public String[] targetValue();

    /**
     * This expression plus each of the expressions is less than the length of the sequence after
     * the method returns the given result. The {@code offset} element must ether be empty or the
     * same length as {@code targetValue}.
     */
    String[] offset() default {};
}
