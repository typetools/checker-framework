package org.checkerframework.framework.flow;

/**
 * The default store used in the Checker Framework.
 *
 * @author Stefan Heule
 *
 */
public class CFStore extends CFAbstractStore<CFValue, CFStore> {

    public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis,
            CFAbstractStore<CFValue, CFStore> other) {
        super(other);
    }

}
