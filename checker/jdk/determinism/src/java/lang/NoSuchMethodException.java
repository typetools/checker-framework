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
 * Thrown when a particular method cannot be found.
 *
 * @author     unascribed
 * @since      JDK1.0
 */
@Det
public
class NoSuchMethodException extends ReflectiveOperationException {
    private static final long serialVersionUID = 5034388446362600923L;

    /**
     * Constructs a <code>NoSuchMethodException</code> without a detail message.
     */
    @Det public NoSuchMethodException() {
        super();
    }

    /**
     * Constructs a <code>NoSuchMethodException</code> with a detail message.
     *
     * @param      s   the detail message.
     */
    @PolyDet public NoSuchMethodException(@PolyDet String s) {
        super(s);
    }
}
