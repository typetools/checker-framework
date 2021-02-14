package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * It is unknown what locks guard the value referred to by the annotated variable. Therefore, the
 * value can never be dereferenced. The locks that guard it might not even be in scope (might be
 * inaccessible) at the location where the {@code @GuardedByUnknown} annotation is written.
 *
 * <p>{@code @GuardedByUnknown} is the top of the GuardedBy qualifier hierarchy. Any value can be
 * assigned into a variable of type {@code @GuardedByUnknown}.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface GuardedByUnknown {}
