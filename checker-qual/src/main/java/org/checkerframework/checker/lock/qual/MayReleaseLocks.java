package org.checkerframework.checker.lock.qual;

import org.checkerframework.framework.qual.InheritedAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method, or one of the methods it calls, might release locks that were held prior to the
 * method being called. You can write this when you are certain the method releases locks, or when
 * you don't know whether the method releases locks.
 *
 * @see ReleasesNoLocks
 * @see LockingFree
 * @see org.checkerframework.dataflow.qual.SideEffectFree
 * @see org.checkerframework.dataflow.qual.Pure
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #lock-lockingfree-example Example use of @MayReleaseLocks
 * @checker_framework.manual #annotating-libraries Annotating libraries
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InheritedAnnotation
public @interface MayReleaseLocks {}
