package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.TypeMirrorConverter;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.lang.model.type.TypeMirror;

/**
 * Checkers should extend a QualAnalysis to customize the TransferFunction for their checker.
 *
 * Currently, the underlying Checker Framework's dataflow does not use this class directly for running the analysis;
 * It only directly uses the QualTransfer. QualAnalysis, QualStore and QualValue act as one way adapters from the
 * qual system to the underlying atm system.
 *
 * For dataflow to actually use this analysis we would need to add functionality for tracking fields and
 * the other functionality that is currently in CFAbstractAnalysis. We could add methods
 * in this class that call back to an CFAbstractAnalysis adapter, like other shims in the system.
 *
 * Because the checker-framework does not directly use this class, adding properties
 * (like initialization) to the QualStore or QualValue will currently have no effect.
 *
 */
public class QualAnalysis<Q> extends Analysis<QualValue<Q>, QualStore<Q>, QualTransfer<Q>> {

    private CFAbstractAnalysis<CFValue, CFStore, CFTransfer> adapter;
    private final QualifierContext<Q> context;
    private final TypeMirrorConverter<Q> converter;

    public QualAnalysis(QualifierContext<Q> context) {

        super(context.getProcessingEnvironment());
        this.context = context;
        this.converter = context.getCheckerAdapter().getTypeMirrorConverter();
    }

    public QualTransfer<Q> createTransferFunction() {
        return new QualTransfer<Q>(this);
    }

    public QualStore<Q> createEmptyStore(boolean sequentialSemantics) {
        return new QualStore<>(this, adapter.createEmptyStore(sequentialSemantics));
    }

    public QualStore<Q> createCopiedStore(QualStore<Q> qualStore) {
        return new QualStore<>(this, adapter.createCopiedStore(qualStore.getUnderlyingStore().copy()));
    }

    public QualValue<Q> createAbstractValue(QualifiedTypeMirror<Q> type) {
        return new QualValue<Q>(type, this);
    }

    @Override
    public QualValue<Q> getValue(Node n) {
        return new QualValue<>(converter.getQualifiedType(adapter.getValue(n).getType()), this);
    }

    public QualValue<Q> createSingleAnnotationValue(Q qual, TypeMirror underlyingType) {
        CFValue atm = adapter.createSingleAnnotationValue(converter.getAnnotation(qual), underlyingType);
        return new QualValue<>(converter.getQualifiedType(atm.getType()), this);
    }

    public QualifierContext<Q> getContext() {
        return context;
    }

    // **********************************************************************
    // Methods for CF adaption.
    // **********************************************************************

    public void setAdapter(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> adapter) {
        this.adapter = adapter;
    }

    /*package*/ QualStore<Q> createCopiedStore(CFStore cfStore) {
        return new QualStore<>(this, cfStore.copy());
    }

    /*package*/ QualStore<Q> createStore(CFStore cfStore) {
        return new QualStore<>(this, cfStore);
    }

    /*package*/ CFAbstractAnalysis<CFValue, CFStore, CFTransfer> getCFAnalysis() {
        return adapter;
    }

    /*package*/ TypeMirrorConverter<Q> getConverter() {
        return converter;
    }
}
