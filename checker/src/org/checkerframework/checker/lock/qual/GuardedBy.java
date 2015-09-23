/*
 * Copyright (c) 2005 Brian Goetz and Tim Peierls
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 *
 * Any republication or derived work distributed in source code form
 * must include this copyright and license notice.
 */
/*
 * Modified for use with the Checker Framework Lock Checker
 */

package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Indicates that a thread may dereference the value referred to by the
 * annotated variable only if the thread holds all the given lock expressions.
 * <p>
 *
 * <code>@GuardedBy({})</code> is the default type qualifier.
 * <p>
 *
 * The argument is a string or set of strings that indicates the expression(s) that must be held,
 * using the <a href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">syntax
 * of Java expressions</a> described in the manual.
 * Expressions evaluate to an intrinsic (built-in, synchronization)
 * monitor, or an explicit {@link java.util.concurrent.locks.Lock}
 * <p>
 *
 * @see Holding
 * @checker_framework.manual #lock-checker Lock Checker
 */
@TypeQualifier
@SubtypeOf(GuardedByInaccessible.class)
@Documented
@DefaultQualifierInHierarchy
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
public @interface GuardedBy {
    /**
     * The Java value expressions that need to be held.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value() default {};
}
