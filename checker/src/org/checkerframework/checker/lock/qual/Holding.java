package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

/**
 * Indicates that when the method is invoked, the given locks must be held
 * by the caller.
 * <p>
 *
 * The possible annotation parameter values are explained in {@link GuardedBy}.
 *
 * @see GuardedBy
 * @checker_framework_manual #lock-checker Lock Checker
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Holding {
    String[] value();
}
