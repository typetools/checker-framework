package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * Indicates a method pre and postcondition: the method expects the
 * specified expressions to be @LockHeld when the annotated method
 * is invoked. It is also expected for the specified expressions
 * to be @LockHeld when exiting the annotated method.
 *
 * The possible annotation parameter values are explained in {@link GuardedBy}.
 *
 * @see GuardedBy
 * @see HoldingOnEntry
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@PreconditionAnnotation(qualifier = LockHeld.class)
@PostconditionAnnotation(qualifier = LockHeld.class)
public @interface Holding {
    /**
     * The Java expressions that need to be {@link LockHeld}.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value();
}
