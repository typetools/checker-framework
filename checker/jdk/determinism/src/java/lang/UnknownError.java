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
 * Thrown when an unknown but serious exception has occurred in the
 * Java Virtual Machine.
 *
 * @author unascribed
 * @since   JDK1.0
 */
@Det
public
class UnknownError extends VirtualMachineError {
    private static final long serialVersionUID = 2524784860676771849L;

    /**
     * Constructs an <code>UnknownError</code> with no detail message.
     */
    @Det public UnknownError() {
        super();
    }

    /**
     * Constructs an <code>UnknownError</code> with the specified detail
     * message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public UnknownError(@PolyDet String s) {
        super(s);
    }
}
