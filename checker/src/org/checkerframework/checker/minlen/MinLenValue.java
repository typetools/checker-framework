package org.checkerframework.checker.minlen;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class MinLenValue extends CFAbstractValue<MinLenValue> {

    public MinLenValue(CFAbstractAnalysis<MinLenValue, ?, ?> analysis, AnnotatedTypeMirror type) {
        super(analysis, type);
    }
}
