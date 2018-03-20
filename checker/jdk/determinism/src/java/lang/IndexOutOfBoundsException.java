/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown to indicate that an index of some sort (such as to an array, to a
 * string, or to a vector) is out of range.
 * <p>
 * Applications can subclass this class to indicate similar exceptions.
 *
 * @author  Frank Yellin
 * @since   JDK1.0
 */
@Det
public
class IndexOutOfBoundsException extends RuntimeException {
    private static final long serialVersionUID = 234122996006267687L;

    /**
     * Constructs an <code>IndexOutOfBoundsException</code> with no
     * detail message.
     */
    @Det public IndexOutOfBoundsException() {
        super();
    }

    /**
     * Constructs an <code>IndexOutOfBoundsException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public IndexOutOfBoundsException(@PolyDet String s) {
        super(s);
    }
}
