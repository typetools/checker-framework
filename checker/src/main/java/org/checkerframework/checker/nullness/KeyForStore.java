package org.checkerframework.checker.nullness;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

public class KeyForStore extends CFAbstractStore<KeyForValue, KeyForStore> {
    public KeyForStore(
            CFAbstractAnalysis<KeyForValue, KeyForStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    protected KeyForStore(CFAbstractStore<KeyForValue, KeyForStore> other) {
        super(other);
    }
}
