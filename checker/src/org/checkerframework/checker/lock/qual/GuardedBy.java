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
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Indicates that a thread may dereference the value referred to by the
 * annotated variable only if the thread holds all the given lock expression.
 * <p>
 *
 * Expressions may evaluate to an intrinsic (built-in, synchronization)
 * monitor, or an explicit {@link java.util.concurrent.locks.Lock}
 * <p>
 *
 * <code>@GuardedBy({})</code> is the default type qualifier.
 * <p>
 *
 * The argument is a string or set of strings that indicates the expression(s) that must be held:
 * <ul>
 * <li>
 * <code>this</code> : The intrinsic lock, or monitor, of the receiver of the method in which the type appears.  For a field, [[TODO]].
 * </li>
 * <li>
 * <code><em>class-name</em>.this</code> : For inner classes, it may be necessary to disambiguate 'this';
 * the <code><em>class-name</em>.this</code> designation allows you to specify which 'this' reference is intended
 * </li>
 * <li>
 * <code>itself</code> : Applicable only to a reference (non-primitive) field; the field's value.
 * </li>
 * <li>
 * <code><em>field-name</em></code> : The instance or static field
 * specified by <code><em>field-name</em></code>.
 * </li>
 * <li>
 * <code><em>class-name</em>.<em>field-name</em></code> : The static field specified
 * by <code><em>class-name</em>.<em>field-name</em></code>.
 * </li>
 * <li>
 * <code><em>method-name</em>()</code> : The value returned by calling the named {@link org.checkerframework.dataflow.qual.Pure} method.
 * </li>
 * <li>
 * <code><em>class-name</em>.class</code> : The Class object for the specified class.
 * </li>
 * </ul>
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
@PreconditionAnnotation(qualifier = LockHeld.class)
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
