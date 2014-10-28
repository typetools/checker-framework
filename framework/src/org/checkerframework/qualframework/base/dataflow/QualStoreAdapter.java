package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * Created by mcarthur on 10/22/14.
 */
public class QualStoreAdapter<Q> extends CFStore {

    public QualStoreAdapter(QualStore<Q> underlying,
            CFAbstractAnalysis<CFValue, CFStore, ?> analysis, boolean sequentialSemantics) {

        super(analysis, sequentialSemantics);
    }
}
