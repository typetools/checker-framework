package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.qualframework.base.TypeMirrorConverter;

import javax.lang.model.element.AnnotationMirror;

/**
 * TODO: qual store creation
 */
public class QualStore<Q> implements Store<QualStore<Q>> {

    private final CFStore adapter;
    private final QualAnalysis<Q> analysis;

    public QualStore(QualAnalysis<Q> analysis, CFStore adapter) {
        this.analysis = analysis;
        this.adapter = adapter;
    }

    @Override
    public QualStore<Q> copy() {
        return analysis.createCopiedStore(adapter);
    }

    @Override
    public QualStore<Q> leastUpperBound(QualStore<Q> other) {
        return new QualStore<>(analysis, adapter.leastUpperBound(other.adapter));
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

    public void insertValue(Receiver r, Q regexAnnotation) {
        adapter.insertValue(r, analysis.getConverter().getAnnotation(regexAnnotation));
    }
}
