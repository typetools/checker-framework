package org.checkerframework.dataflow.cfg.builder;

import javax.lang.model.type.TypeMirror;

/** A tuple with 4 named elements. */
interface TreeInfo {
    boolean isBoxed();

    boolean isNumeric();

    boolean isBoolean();

    TypeMirror unboxedType();
}
