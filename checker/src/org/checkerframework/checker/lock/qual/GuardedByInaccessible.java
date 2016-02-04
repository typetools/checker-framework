package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The top of the GuardedBy qualifier hierarchy.
 * Indicates that the value referred to by the
 * annotated variable can never be dereferenced.
 * <p>
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@SubtypeOf({})
@Retention(RetentionPolicy.RUNTIME)
@DefaultInUncheckedCodeFor({ TypeUseLocation.RECEIVER })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface GuardedByInaccessible {}
