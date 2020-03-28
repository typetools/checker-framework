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
     * @return Java expressions whose values are locks that are held after the method returns the
     *     given result
     * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
     *     Java expressions</a>
     */
    // It would be clearer for users if this field were named "lock".
    // However, method ContractUtils.getConditionalPostconditions in the CF implementation assumes
    // that conditional postconditions have a field named "expression".
    String[] expression();

    /** @return the return value of the method under which the postconditions hold */
    boolean result();
}
