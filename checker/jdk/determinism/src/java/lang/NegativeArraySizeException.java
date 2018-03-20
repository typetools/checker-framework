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
 * Thrown if an application tries to create an array with negative size.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
@Det
public
class NegativeArraySizeException extends RuntimeException {
    private static final long serialVersionUID = -8960118058596991861L;

    /**
     * Constructs a <code>NegativeArraySizeException</code> with no
     * detail message.
     */
    @Det public NegativeArraySizeException() {
        super();
    }

    /**
     * Constructs a <code>NegativeArraySizeException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public NegativeArraySizeException(@PolyDet String s) {
        super(s);
    }
}
