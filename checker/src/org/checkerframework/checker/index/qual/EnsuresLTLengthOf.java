package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

/**
 * Indicates that the value expressions evaluate to an integer whose value is less than the lengths
 * of all the given sequences, if the method terminates successfully.
 *
 * @see LTLengthOf
 * @see org.checkerframework.checker.index.IndexChecker
 * @checker_framework.manual #index-checker Index Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = LTLengthOf.class)
@InheritedAnnotation
public @interface EnsuresLTLengthOf {
    /**
     * The Java expressions that are less than the length of the given sequences on successful
     * method termination.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] value();

    /**
     * Sequences, each of which is longer than the each of the expressions' value on successful
     * method termination.
     */
    @JavaExpression
    @QualifierArgument("value")
    String[] targetValue();

    /**
     * This expression plus each of the value expressions is less than the length of the sequence on
     * successful method termination. The {@code offset} element must ether be empty or the same
     * length as {@code targetValue}.
     */
    @JavaExpression
    @QualifierArgument("offset")
    String[] offset() default {};
}
