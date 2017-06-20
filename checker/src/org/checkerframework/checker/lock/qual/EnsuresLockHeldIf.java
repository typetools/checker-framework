package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the given expressions are held if the method terminates successfully and returns
 * the given result (either true or false).
 *
 * @see EnsuresLockHeld
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #ensureslockheld-examples Example use of @EnsuresLockHeldIf
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = LockHeld.class)
@InheritedAnnotation
public @interface EnsuresLockHeldIf {
    /**
     * Java expressions whose values are held after the method returns the given result.
     *
     * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
     *     Java expressions</a>
     */
    String[] expression();

    /** The return value of the method that needs to hold for the postcondition to hold. */
    boolean result();
}
