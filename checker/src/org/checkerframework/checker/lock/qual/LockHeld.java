package org.checkerframework.checker.lock.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link LockHeld} is a type annotation that indicates that an expression is
 * used as a lock and the lock is known to be held on the current thread.
 *
 * @see LockPossiblyHeld
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifier
@SubtypeOf(LockPossiblyHeld.class) // This is the bottom type in this hierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface LockHeld {
}
