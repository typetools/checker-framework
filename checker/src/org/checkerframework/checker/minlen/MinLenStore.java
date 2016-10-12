package org.checkerframework.checker.minlen;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

public class MinLenStore extends CFAbstractStore<MinLenValue, MinLenStore> {

    protected MinLenStore(MinLenStore other) {
        super(other);
    }
    public MinLenStore(CFAbstractAnalysis<MinLenValue, MinLenStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }
}
