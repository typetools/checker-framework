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
 * Thrown when the "verifier" detects that a class file,
 * though well formed, contains some sort of internal inconsistency
 * or security problem.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
@Det
public
class VerifyError extends LinkageError {
    private static final long serialVersionUID = 7001962396098498785L;

    /**
     * Constructs an <code>VerifyError</code> with no detail message.
     */
    @Det public VerifyError() {
        super();
    }

    /**
     * Constructs an <code>VerifyError</code> with the specified detail message.
     *
     * @param   s   the detail message.
     */
    @PolyDet public VerifyError(@PolyDet String s) {
        super(s);
    }
}
