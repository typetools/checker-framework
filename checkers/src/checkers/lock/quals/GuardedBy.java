/*
 * Copyright (c) 2005 Brian Goetz and Tim Peierls
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 *
 * Any republication or derived work distributed in source code form
 * must include this copyright and license notice.
 */

package checkers.lock.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The field (or other reference) to which this annotation is applied can only be accessed
 * when holding a particular lock, which may be a built-in (synchronization) lock,
 * or may be an explicit {@link java.util.concurrent.locks.Lock}.
 * <p>
 * 
 * This annotation does <b>not</b> indicate whether or not the given lock
 * is held.  It merely indicates that the lock must be held when the
 * reference is accessed.  An unannotated reference is a subtype of a
 * <code>@GuardedBy</code> one, because the unannotated reference may be
 * used in any context where the <code>@GuardedBy</code> reference is.
 * <p>
 * 
 * The argument is a string that indicates which lock guards the annotated variable:
 * <ul>
 * <li>
 * <code>this</code> : The intrinsic lock of the object in whose class the field is defined.
 * </li>
 * <li>
 * <code>class-name.this</code> : For inner classes, it may be necessary to disambiguate 'this';
 * the <em>class-name.this</em> designation allows you to specify which 'this' reference is intended
 * </li>
 * <li>
 * <code>itself</code> : For reference fields only; the object to which the field refers.
 * </li>
 * <li>
 * <code>field-name</code> : The lock object is referenced by the (instance or static) field
 * specified by <em>field-name</em>.
 * </li>
 * <li>
 * <code>class-name.field-name</code> : The lock object is reference by the static field specified
 * by <em>class-name.field-name</em>.
 * </li>
 * <li>
 * <code>method-name()</code> : The lock object is returned by calling the named nil-ary method.
 * </li>
 * <li>
 * <code>class-name.class</code> : The Class object for the specified class should be used as the lock object.
 * </li>
 * </ul>
 *
 * @see Holding
 */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifier
@SubtypeOf({})
public @interface GuardedBy {
    String value();
}
