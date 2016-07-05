package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class LowerBoundValue extends CFAbstractValue<LowerBoundValue> {

    public LowerBoundValue(CFAbstractAnalysis<LowerBoundValue, ?, ?> analysis, AnnotatedTypeMirror type) {
        super(analysis, type);
    }

}
