package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractTransfer;

public class LowerBoundTransfer extends CFAbstractTransfer<LowerBoundValue, LowerBoundStore, LowerBoundTransfer> {
    protected LowerBoundAnalysis analysis;

    public LowerBoundTransfer(LowerBoundAnalysis analysis) {
	super(analysis);
	this.analysis = analysis;
    }
}
