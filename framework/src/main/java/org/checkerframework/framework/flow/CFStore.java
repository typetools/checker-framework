package org.checkerframework.framework.flow;

/** The default store used in the Checker Framework. */
public class CFStore extends CFAbstractStore<CFValue, CFStore> {

    public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    /**
     * Copy constructor.
     *
     * @param other the CFStore to copy
     */
    public CFStore(CFAbstractStore<CFValue, CFStore> other) {
        super(other);
    }
}
