package org.checkerframework.checker.index.upperbound;

import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;

public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {
    public UpperBoundStore(
            CFAbstractAnalysis<CFValue, UpperBoundStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public UpperBoundStore(
            CFAbstractAnalysis<CFValue, UpperBoundStore, ?> analysis,
            CFAbstractStore<CFValue, UpperBoundStore> other) {
        super(other);
    }

    @Override
    public void updateForAssignment(Node n, /*@Nullable*/ CFValue val) {
        // Do reassignment things here.

        super.updateForAssignment(n, val);
    }
}
