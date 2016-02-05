package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * It is unknown what locks guard the value referred to by the annotated
 * variable.  Therefore, the value can never be dereferenced.  The locks
 * that guard it might not even be in scope (might be inaccessible) at the
 * location where the <tt>@GuardedByInaccessible</tt> annotation is
 * written.
 * <p>
 *
 * <tt>@GuardedByInaccessible</tt> is the top of the GuardedBy qualifier
 * hierarchy.  Any value can be assigned into a variable of type
 * <tt>@GuardedByInaccessible</tt>.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@SubtypeOf({})
@Retention(RetentionPolicy.RUNTIME)
@DefaultInUncheckedCodeFor({ TypeUseLocation.RECEIVER })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface GuardedByInaccessible {}
