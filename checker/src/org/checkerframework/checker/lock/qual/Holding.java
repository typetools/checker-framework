package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * Indicates a method precondition: the method expects the
 * specified expressions to be held when the annotated method
 * is invoked.
 * <p>
 *
 * The possible annotation parameter values are explained in {@link GuardedBy}.
 *
 * @see GuardedBy
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@PreconditionAnnotation(qualifier = LockHeld.class)
public @interface Holding {
    /**
     * The Java value expressions that need to be held.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value();
}
