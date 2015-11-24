package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * The bottom of the GuardedBy qualifier hierarchy.
 * If a variable {@code x} has type {@code @GuardedByBottom}, then
 * the value referred to by {@code x} is {@code null}.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({GuardedBy.class, GuardSatisfied.class, net.jcip.annotations.GuardedBy.class, javax.annotation.concurrent.GuardedBy.class})
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL})
@Documented
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@Target({}) // not necessary to be used by the programmer
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardedByBottom {}
