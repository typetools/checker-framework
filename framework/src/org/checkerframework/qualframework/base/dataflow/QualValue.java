package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;

public class QualValue<Q> implements AbstractValue<QualValue<Q>> {

    private final QualifiedTypeMirror<Q> value;
    private final QualAnalysis<Q> analysis;

    public QualValue(QualifiedTypeMirror<Q> value,
            QualAnalysis<Q> analysis) {

        this.analysis = analysis;
        this.value = value;
    }

    @Override
    public QualValue<Q> leastUpperBound(QualValue<Q> other) {
        return analysis.createAbstractValue(
                analysis.getConverter().getQualifiedType(
                        analysis.getCFAnalysis().createAbstractValue(analysis.getConverter().getAnnotatedType(value)).leastUpperBound(
                        analysis.getCFAnalysis().createAbstractValue(analysis.getConverter().getAnnotatedType(other.getType()))).getType()));
    }

    public QualifiedTypeMirror<Q> getType() {
        return value;
    }
}