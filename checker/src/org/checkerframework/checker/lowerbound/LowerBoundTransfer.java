package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

public class LowerBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, LowerBoundTransfer> {
    protected LowerBoundAnalysis analysis;

    public LowerBoundTransfer(LowerBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
    }
}
