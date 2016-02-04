package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * The method does not release any locks that were held prior
 * to the method call, nor do any of the methods that it calls.
 * The method might acquire locks but then release them, or might
 * acquire locks but not release them (in which case it should
 * also be annotated with {@literal @}{@link EnsuresLockHeld} or
 * {@literal @}{@link EnsuresLockHeldIf}).
 * <p>
 * This is the default for methods being type-checked that have no {@code @LockingFree},
 * {@code @MayReleaseLocks}, {@code @SideEffectFree}, or {@code @Pure}
 * annotation.
 * <p>
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
