package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation represents a predicate on @CalledMethods annotations that must be true. It is
 * intended to extend the possible LUB of the @CalledMethods type system to permit methods to be
 * annotated to require a disjunction of method calls. So, for instance, if it was acceptable to
 * call method a() or method b() before calling method c(), then c()'s receiver should have
 * an @CalledMethodsPredicate annotation with the argument "a || b"
 *
 * <p>The argument is a string. The string must be constructed from the following grammar:
 *
 * <p>S &rarr; method name | (S) | S &amp;&amp; S | S || S
 *
 * <p>That is, the permitted elements are method names, parentheses, and the strings "&amp;&amp;"
 * and "||". "&amp;&amp;" has higher precedence than "||", following standard Java operator
 * semantics.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({CalledMethods.class})
public @interface CalledMethodsPredicate {
    String value();
}
