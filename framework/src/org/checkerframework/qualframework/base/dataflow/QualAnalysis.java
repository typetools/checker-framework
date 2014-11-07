package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.TypeMirrorConverter;
import org.checkerframework.qualframework.util.QualifierContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

/**
 * Checkers should extend a QualAnalysis to customize the TransferFunction for their checker.
 *
 * The underlying checker-framework's dataflow does not use this class directly for running the analysis;
 * It does use the QualTransfer.
 *
 * For dataflow to actually use this analysis we would need to add functionality for tracking fields and
 * the other functionality that is in CFAbstractAnalysis. We could add methods
 * in this class that call back to an CFAbstractAnalysis adapter, like other shims in system.
 *
 * Also, because the checker-framework does not directly use this class, adding properties
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

    public void setAdapter(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> adapter) {
        this.adapter = adapter;
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

    public QualStore<Q> createCopiedStore(CFStore cfStore) {
        return new QualStore<>(this, cfStore.copy());
    }

    public QualValue<Q> createAbstractValue(QualifiedTypeMirror<Q> type) {
        return new QualValue<Q>(type, this);
    }

    public CFAbstractAnalysis<CFValue, CFStore, CFTransfer> getCFAnalysis() {
        return adapter;
    }

    public QualifierContext<Q> getContext() {
        return context;
    }

    public TypeMirrorConverter<Q> getConverter() {
        return converter;
    }

    /**
     * Returns an abstract value containing an annotated type with the
     * annotation {@code anno}, and 'top' for all other hierarchies. The
     * underlying type is {@link Object}.
     */
    public QualValue<Q> createSingleAnnotationValue(Q qual,
            TypeMirror underlyingType) {
        CFValue atm = adapter.createSingleAnnotationValue(converter.getAnnotation(qual), underlyingType);
        return new QualValue<>(converter.getQualifiedType(atm.getType()), this);
    }

    @Override
    public QualValue<Q> getValue(Node n) {
        return new QualValue<>(converter.getQualifiedType(adapter.getValue(n).getType()), this);
    }
}
