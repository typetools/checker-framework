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
 * Thrown when a stack overflow occurs because an application
 * recurses too deeply.
 *
 * @author unascribed
 * @since   JDK1.0
 */
@Det
public
class StackOverflowError extends VirtualMachineError {
    private static final long serialVersionUID = 8609175038441759607L;

    /**
     * Constructs a <code>StackOverflowError</code> with no detail message.
     */
    @Det public StackOverflowError() {
        super();
    }

    /**
     * Constructs a <code>StackOverflowError</code> with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public StackOverflowError(@PolyDet String s) {
        super(s);
    }
}
