package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;


public class LowerBoundStore extends CFAbstractStore<LowerBoundValue, LowerBoundStore> {

    protected LowerBoundStore(LowerBoundStore other) {
        super(other);
    }
    public LowerBoundStore(CFAbstractAnalysis<LowerBoundValue, LowerBoundStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }
}
