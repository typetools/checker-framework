/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown when the Java Virtual Machine cannot allocate an object
 * because it is out of memory, and no more memory could be made
 * available by the garbage collector.
 *
 * {@code OutOfMemoryError} objects may be constructed by the virtual
 * machine as if {@linkplain Throwable#Throwable(String, Throwable,
 * boolean, boolean) suppression were disabled and/or the stack trace was not
 * writable}.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
@Det
public class OutOfMemoryError extends VirtualMachineError {
    private static final long serialVersionUID = 8228564086184010517L;

    /**
     * Constructs an {@code OutOfMemoryError} with no detail message.
     */
    @Det public OutOfMemoryError() {
        super();
    }

    /**
     * Constructs an {@code OutOfMemoryError} with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public OutOfMemoryError(@PolyDet String s) {
        super(s);
    }
}
