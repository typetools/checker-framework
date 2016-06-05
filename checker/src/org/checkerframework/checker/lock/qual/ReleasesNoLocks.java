package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * The method, and all the methods it calls, maintain a strictly
 * nondecreasing lock hold count on the current thread for any locks
 * that were held prior to the method call.
 * The method might acquire locks but then release them, or might
 * acquire locks but not release them (in which case it should
 * also be annotated with {@literal @}{@link EnsuresLockHeld} or
 * {@literal @}{@link EnsuresLockHeldIf}).
 * <p>
 * This is the default for methods being type-checked that have no
 * {@code @}{@link LockingFree}, {@code @}{@link MayReleaseLocks},
 * {@code @}{@link SideEffectFree}, or {@code @}{@link Pure}
 * annotation.
 * <p>
 * {@code @ReleasesNoLocks} provides a guarantee unlike
 * {@code @}{@link MayReleaseLocks}, which provides no guarantees.
 * However, {@code @ReleasesNoLocks} provides a weaker guarantee than
 * {@code @}{@link LockingFree}.
 *
 * @see MayReleaseLocks
 * @see LockingFree
 * @see SideEffectFree
 * @see Pure
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #lock-lockingfree-example Example use of @ReleasesNoLocks
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@InheritedAnnotation
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface ReleasesNoLocks {
}
