/*
 * Copyright 1995-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Thrown when an application tries to load in a class through its
 * string name using:
 * <ul>
 * <li>The <code>forName</code> method in class <code>Class</code>.
 * <li>The <code>findSystemClass</code> method in class
 *     <code>ClassLoader</code> .
 * <li>The <code>loadClass</code> method in class <code>ClassLoader</code>.
 * </ul>
 * <p>
 * but no definition for the class with the specified name could be found.
 *
 * <p>As of release 1.4, this exception has been retrofitted to conform to
 * the general purpose exception-chaining mechanism.  The "optional exception
 * that was raised while loading the class" that may be provided at
 * construction time and accessed via the {@link #getException()} method is
 * now known as the <i>cause</i>, and may be accessed via the {@link
 * Throwable#getCause()} method, as well as the aforementioned "legacy method."
 *
 * @author  unascribed
 * @see     Class#forName(String)
 * @see     ClassLoader#findSystemClass(String)
 * @see     ClassLoader#loadClass(String, boolean)
 * @since   JDK1.0
 */
public class ClassNotFoundException extends Exception {
    /**
     * use serialVersionUID from JDK 1.1.X for interoperability
     */
     private static final long serialVersionUID = 9176873029745254542L;

    /**
     * This field holds the exception ex if the
     * ClassNotFoundException(@Nullable String s, @Nullable Throwable ex) constructor was
     * used to instantiate the object
     * @serial
     * @since 1.2
     */
    private Throwable ex;

    /**
     * Constructs a <code>ClassNotFoundException</code> with no detail message.
     */
    @SideEffectFree
    public ClassNotFoundException() {
        super((Throwable)null);  // Disallow initCause
    }

    /**
     * Constructs a <code>ClassNotFoundException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    @SideEffectFree
    public ClassNotFoundException(@Nullable String s) {
        super(s, null);  //  Disallow initCause
    }

    /**
     * Constructs a <code>ClassNotFoundException</code> with the
     * specified detail message and optional exception that was
     * raised while loading the class.
     *
     * @param s the detail message
     * @param ex the exception that was raised while loading the class
     * @since 1.2
     */
    @SideEffectFree
    public ClassNotFoundException(@Nullable String s, @Nullable Throwable ex) {
        super(s, null);  //  Disallow initCause
        this.ex = ex;
    }

    /**
     * Returns the exception that was raised if an error occurred while
     * attempting to load the class. Otherwise, returns <tt>null</tt>.
     *
     * <p>This method predates the general-purpose exception chaining facility.
     * The {@link Throwable#getCause()} method is now the preferred means of
     * obtaining this information.
     *
     * @return the <code>Exception</code> that was raised while loading a class
     * @since 1.2
     */
    public Throwable getException() {
        return ex;
    }

    /**
     * Returns the cause of this exception (the exception that was raised
     * if an error occurred while attempting to load the class; otherwise
     * <tt>null</tt>).
     *
     * @return  the cause of this exception.
     * @since   1.4
     */
    public Throwable getCause() {
        return ex;
    }
}
