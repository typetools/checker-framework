package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.framework.flow.CFStore;

/**
 * Created by mcarthur on 10/22/14.
 */
public class QualStore<Q> implements Store<QualStore<Q>> {

    private final CFStore adapter;

    public QualStore(CFStore adapter) {
        this.adapter = adapter;
    }

    @Override
    public QualStore<Q> copy() {
        return new QualStore<>(adapter.copy());
    }

    @Override
    public QualStore<Q> leastUpperBound(QualStore<Q> other) {
        return new QualStore<>(adapter.leastUpperBound(other.adapter));
    }

    @Override
    public boolean canAlias(Receiver a, Receiver b) {
        return adapter.canAlias(a, b);
    }

    @Override
    public boolean hasDOToutput() {
        return adapter.hasDOToutput();
    }

    @Override
    public String toDOToutput() {
        return adapter.toDOToutput();
    }

    public CFStore getUnderlyingStore() {
        return adapter;
    }
}
