package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation represents a predicate on {@code @}{@link CalledMethods} annotations. If method
 * {@code c()}'s receiver type is annotated with {@code @CalledMethodsPredicate("a || b")}, then it
 * is acceptable to call either method {@code a()} or method {@code b()} before calling method
 * {@code c()}.
 *
 * @checker_framework.manual #called-methods-checker Called Methods Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({CalledMethods.class})
public @interface CalledMethodsPredicate {
    /**
     * A boolean expression constructed from the following grammar:
     *
     * <p>S &rarr; method name | S &amp;&amp; S | S || S | !S | (S)
     *
     * <p>The expression uses standard Java operator precedence: "!" then "&amp;&amp;" then "||".
     *
     * @return the boolean expression
     */
    String value();
}
