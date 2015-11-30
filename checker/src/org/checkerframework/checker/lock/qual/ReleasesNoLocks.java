package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * The method neither acquires nor releases locks -- nor do any of the methods that it calls.
 * <p>
 *
 * {@code @ReleasesNoLocks} provides a guarantee unlike {@code @MayReleaseLocks}, which provides
 * no guarantees.  However, {@code @ReleasesNoLocks} provides a weaker guarantee than {@code @LockingFree}.
 *
 * @see MayReleaseLocks
 * @see LockingFree
 * @see org.checkerframework.dataflow.qual.SideEffectFree
 * @see org.checkerframework.dataflow.qual.Pure
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@InheritedAnnotation
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface ReleasesNoLocks {
}
