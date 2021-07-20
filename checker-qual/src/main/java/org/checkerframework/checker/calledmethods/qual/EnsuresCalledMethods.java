package org.checkerframework.checker.calledmethods.qual;

import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.QualifierArgument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the method, if it terminates successfully, always invokes the given methods on the
 * given expressions.
 *
 * <p>Consider the following method:
 *
 * <pre>
 * &#64;EnsuresCalledMethods(value = "#1", methods = "m")
 * public void callM(T t) { ... }
 * </pre>
 *
 * <p>This method guarantees that {@code t.m()} is always called before the method returns.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@PostconditionAnnotation(qualifier = CalledMethods.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface EnsuresCalledMethods {
    /**
     * The Java expressions to which the qualifier applies.
     *
     * @return the Java expressions to which the qualifier applies
     * @see org.checkerframework.framework.qual.EnsuresQualifier
     */
    // Postconditions must use "value" as the name (conditional postconditions use "expression").
    String[] value();

    /**
     * The methods guaranteed to be invoked on the expressions.
     *
     * @return the methods guaranteed to be invoked on the expressions
     */
    @QualifierArgument("value")
    String[] methods();
}
