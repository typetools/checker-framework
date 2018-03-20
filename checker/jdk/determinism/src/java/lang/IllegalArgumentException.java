/*
 * Copyright (c) 1994, 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;

import org.checkerframework.checker.determinism.qual.*;
/**
 * Thrown to indicate that a method has been passed an illegal or
 * inappropriate argument.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
@Det
public
class IllegalArgumentException extends RuntimeException {
    /**
     * Constructs an <code>IllegalArgumentException</code> with no
     * detail message.
     */
    @Det public IllegalArgumentException() {
        super();
    }

    /**
     * Constructs an <code>IllegalArgumentException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public IllegalArgumentException(@PolyDet String s) {
        super(s);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * <p>Note that the detail message associated with <code>cause</code> is
     * <i>not</i> automatically incorporated in this exception's detail
     * message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link Throwable#getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link Throwable#getCause()} method).  (A <tt>null</tt> value
     *         is permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since 1.5
     */
    @PolyDet public IllegalArgumentException(@PolyDet String message, @PolyDet Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link Throwable#getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.5
     */
    @PolyDet public IllegalArgumentException(@PolyDet Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = -5365630128856068164L;
}
