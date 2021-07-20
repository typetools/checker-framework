package org.checkerframework.checker.lock.qual;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.InheritedAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method neither acquires nor releases locks, nor do any of the methods that it calls. More
 * specifically, the method is not {@code synchronized}, it contains no {@code synchronized} blocks,
 * it contains no calls to {@code lock} or {@code unlock}, and it contains no calls to other
 * non-{@code @LockingFree} methods.
 *
 * <p>{@code @LockingFree} provides a stronger guarantee than {@code @}{@link ReleasesNoLocks} and a
 * weaker guarantee than {@code @}{@link SideEffectFree}.
 *
 * @see MayReleaseLocks
 * @see ReleasesNoLocks
 * @see SideEffectFree
 * @see Pure
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #lock-lockingfree-example Example use of @LockingFree
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InheritedAnnotation
public @interface LockingFree {}
