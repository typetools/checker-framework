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

import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * The field (or other variable) to which this annotation is applied can
 * only be accessed when holding a particular lock, which may be a built-in
 * (synchronization) lock, or may be an explicit {@link
 * java.util.concurrent.locks.Lock}.
 * <p>
 *
 * This annotation does <b>not</b> indicate whether or not the given lock
 * is held at the moment that execution reaches the annotation.
 * It merely indicates that the lock must be held when the
 * variable is accessed.
 * <p>
 *
 * The argument is a string that indicates which lock guards the annotated variable:
 * <ul>
 * <li>
 * <code>this</code> : The intrinsic lock of the object in whose class the field is defined.
 * </li>
 * <li>
 * <code><em>class-name</em>.this</code> : For inner classes, it may be necessary to disambiguate 'this';
 * the <code><em>class-name</em>.this</code> designation allows you to specify which 'this' reference is intended
 * </li>
 * <li>
 * <code>itself</code> : For reference (non-primitive) fields only; the object to which the field refers.
 * </li>
 * <li>
 * <code><em>field-name</em></code> : The lock object is referenced by the (instance or static) field
 * specified by <code><em>field-name</em></code>.
 * </li>
 * <li>
 * <code><em>class-name</em>.<em>field-name</em></code> : The lock object is reference by the static field specified
 * by <code><em>class-name</em>.<em>field-name</em></code>.
 * </li>
 * <li>
 * <code><em>method-name</em>()</code> : The lock object is returned by calling the named nil-ary method.
 * </li>
 * <li>
 * <code><em>class-name</em>.class</code> : The Class object for the specified class should be used as the lock object.
 * </li>
 * </ul>
 *
 * <b>Subtyping rules:</b>
 * An unannotated type is a subtype of a
 * <code>@GuardedBy</code> one, because the unannotated type may be
 * used in any context where the <code>@GuardedBy</code> one is.
 * <p>
 *
 * @see Holding
 * @see HoldingOnEntry
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
@PreconditionAnnotation(qualifier = LockHeld.class)
public @interface GuardedBy {
    /**
     * The Java expressions which need to be {@link LockHeld}.
     *
     * @see <a
     *      href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax
     *      of Java expressions</a>
     */
    String[] value();
}
