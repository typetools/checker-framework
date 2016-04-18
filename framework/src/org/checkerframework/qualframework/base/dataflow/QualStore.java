package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.framework.flow.CFStore;

/**
 * QualStore is a {@link Store} for quals.
 *
 * It proxies a {@link CFStore} adapter.
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
        return analysis.createStore(adapter.leastUpperBound(other.adapter));
    }

    @Override
    public boolean canAlias(Receiver a, Receiver b) {
        return adapter.canAlias(a, b);
    }

    public CFStore getUnderlyingStore() {
        return adapter;
    }

    public void insertValue(Receiver r, Q regexAnnotation) {
        adapter.insertValue(r, analysis.getConverter().getAnnotation(regexAnnotation));
    }

    public void visualize(CFGVisualizer<?, QualStore<Q>, ?> viz) {
        // TODO: this method currently isn't called. The corresponding
        // method on the adapter is called by the AnnotatedTypeFactory.
        // In the future, the QualifiedTypeFactory should call this method
        // and something like the following might work:
        // CFGVisualizer<?, ?, ?> adaptViz = (CFGVisualizer<?, ?, ?>) viz;
        // this.adapter.visualize((CFGVisualizer <?, CFStore, ?>) adaptViz);
        // however, the mismatch between the QualStore<Q> and the
        // CFStore might require some further refactorings.
    }
}
