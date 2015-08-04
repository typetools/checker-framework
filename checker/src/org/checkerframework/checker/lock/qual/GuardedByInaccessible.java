package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The top of the guarded-by qualifier hierarchy.
 *
 * Indicates that the value referred to by the
 * annotated variable can never be accessed.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Documented
@Target({}) // not necessary to be used by the programmer
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardedByInaccessible {}
