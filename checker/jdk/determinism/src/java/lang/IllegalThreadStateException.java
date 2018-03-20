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
 * Thrown to indicate that a thread is not in an appropriate state
 * for the requested operation. See, for example, the
 * <code>suspend</code> and <code>resume</code> methods in class
 * <code>Thread</code>.
 *
 * @author  unascribed
 * @see     java.lang.Thread#resume()
 * @see     java.lang.Thread#suspend()
 * @since   JDK1.0
 */
@Det
public class IllegalThreadStateException extends IllegalArgumentException {
    private static final long serialVersionUID = -7626246362397460174L;

    /**
     * Constructs an <code>IllegalThreadStateException</code> with no
     * detail message.
     */
    @Det public IllegalThreadStateException() {
        super();
    }

    /**
     * Constructs an <code>IllegalThreadStateException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public IllegalThreadStateException(@PolyDet String s) {
        super(s);
    }
}
