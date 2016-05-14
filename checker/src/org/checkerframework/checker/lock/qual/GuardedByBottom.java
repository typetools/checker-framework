package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom of the GuardedBy qualifier hierarchy.
 * If a variable {@code x} has type {@code @GuardedByBottom}, then
 * the value referred to by {@code x} is {@code null} and can never
 * be dereferenced.
 * <p>
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@SubtypeOf({GuardedBy.class, GuardSatisfied.class})
@ImplicitFor(literals = { LiteralKind.NULL })
@Documented
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardedByBottom {}
