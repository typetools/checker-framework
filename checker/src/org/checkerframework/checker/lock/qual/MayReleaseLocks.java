package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * It is unknown whether the method, or one of the methods it calls, releases locks
 * that were held prior to the method being called.
 * <p>
 *
 * {@code @MayReleaseLocks} is the conservative default for methods in unannotated libraries.
 *
 * @see ReleasesNoLocks
 * @see LockingFree
 * @see org.checkerframework.dataflow.qual.SideEffectFree
 * @see org.checkerframework.dataflow.qual.Pure
 *
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #annotating-libraries Annotating libraries
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface MayReleaseLocks {
}
