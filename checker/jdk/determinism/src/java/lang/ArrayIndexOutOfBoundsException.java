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
 * Thrown to indicate that an array has been accessed with an
 * illegal index. The index is either negative or greater than or
 * equal to the size of the array.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
@Det
public
class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = -5116101128118950844L;

    /**
     * Constructs an <code>ArrayIndexOutOfBoundsException</code> with no
     * detail message.
     */
    @Det public ArrayIndexOutOfBoundsException() {
        super();
    }

    /**
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code>
     * class with an argument indicating the illegal index.
     *
     * @param   index   the illegal index.
     */
    @PolyDet public ArrayIndexOutOfBoundsException(@PolyDet int index) {
        super("Array index out of range: " + index);
    }

    /**
     * Constructs an <code>ArrayIndexOutOfBoundsException</code> class
     * with the specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public ArrayIndexOutOfBoundsException(@PolyDet String s) {
        super(s);
    }
}
