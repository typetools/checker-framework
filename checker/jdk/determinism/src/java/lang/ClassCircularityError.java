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
 * Thrown when the Java Virtual Machine detects a circularity in the
 * superclass hierarchy of a class being loaded.
 *
 * @author     unascribed
 * @since      JDK1.0
 */
@Det
public class ClassCircularityError extends LinkageError {
    private static final long serialVersionUID = 1054362542914539689L;

    /**
     * Constructs a {@code ClassCircularityError} with no detail message.
     */
    @Det public ClassCircularityError() {
        super();
    }

    /**
     * Constructs a {@code ClassCircularityError} with the specified detail
     * message.
     *
     * @param  s
     *         The detail message
     */
    @PolyDet public ClassCircularityError(@PolyDet String s) {
        super(s);
    }
}
