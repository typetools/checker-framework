/*
 * Copyright (c) 1994, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown if the Java Virtual Machine cannot find an appropriate
 * native-language definition of a method declared <code>native</code>.
 *
 * @author unascribed
 * @see     java.lang.Runtime
 * @since   JDK1.0
 */
@Det
public
class UnsatisfiedLinkError extends LinkageError {
    private static final long serialVersionUID = -4019343241616879428L;

    /**
     * Constructs an <code>UnsatisfiedLinkError</code> with no detail message.
     */
    @Det public UnsatisfiedLinkError() {
        super();
    }

    /**
     * Constructs an <code>UnsatisfiedLinkError</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public UnsatisfiedLinkError(@PolyDet String s) {
        super(s);
    }
}
