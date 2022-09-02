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
 * <p>If a class has any {@code @}{@link org.checkerframework.checker.mustcall.qual.Owning Owning}
 * fields, then one or more of its must-call methods should be annotated to indicate that the
 * must-call obligations are satisfied. The must-call methods are those named by the {@code @}{@link
 * org.checkerframework.checker.mustcall.qual.MustCall MustCall} or {@code @}{@link
 * org.checkerframework.checker.mustcall.qual.InheritableMustCall InheritableMustCall} annotation on
 * the class declaration, such as {@code close()}. Here is a common example:
 *
 * <pre>
 * &#64;EnsuresCalledMethods(value = {"owningField1", "owningField2"}, methods = "close")
 * public void close() { ... }
 * </pre>
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
