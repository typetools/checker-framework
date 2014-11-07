package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * Created by mcarthur on 10/23/14.
 */
public class QualTransferResult<Q> extends TransferResult<QualValue<Q>, QualStore<Q>> {

    private TransferResult<CFValue, CFStore> adapter;

    public QualTransferResult(QualValue<Q> result,
            TransferResult<CFValue, CFStore> adapter) {
        super(result);
        this.adapter = adapter;
    }

    @Override
    public QualStore<Q> getRegularStore() {
        adapter.getRegularStore();
        return null;
    }

    @Override
    public QualStore<Q> getThenStore() {
        return null;
    }

    @Override
    public QualStore<Q> getElseStore() {
        return null;
    }

    @Override
    public boolean containsTwoStores() {
        return false;
    }

    @Override
    public boolean storeChanged() {
        return false;
    }
}
