package org.checkerframework.qualframework.base.dataflow;

import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.qualframework.base.TypeMirrorConverter;

/**
 * QualValue is an {@link AbstractValue} for quals.
 *
 * {@link CFAnalysis} is used to perform the leastUpperBound operation.
 */
public class QualValue<Q> implements AbstractValue<QualValue<Q>> {

    private final Q qualifier;
    private final TypeMirror underlyingType;
    private final QualAnalysis<Q> analysis;

    public QualValue(Q qualifier, TypeMirror underlyingType, QualAnalysis<Q> analysis) {

        this.analysis = analysis;
        this.qualifier = qualifier;
        this.underlyingType = underlyingType;
    }

    @Override
    public QualValue<Q> leastUpperBound(QualValue<Q> other) {
        CFAbstractAnalysis<CFValue, CFStore, CFTransfer> cfAnalysis = analysis.getCFAnalysis();
        TypeMirrorConverter<Q> converter = analysis.getConverter();

        Set<AnnotationMirror> otherAnnos =
                Collections.singleton(converter.getAnnotation(other.qualifier));
        Set<AnnotationMirror> thisAnnos =
                Collections.singleton(converter.getAnnotation(this.qualifier));

        CFValue otherCF = cfAnalysis.createAbstractValue(otherAnnos, other.underlyingType);
        CFValue thisCF = cfAnalysis.createAbstractValue(thisAnnos, underlyingType);
        CFValue lub = thisCF.leastUpperBound(otherCF);
        return analysis.createAbstractValue(lub.getAnnotations(), lub.getUnderlyingType());
    }

    public Q getQualifier() {
        return qualifier;
    }

    public TypeMirror getUnderlyingType() {
        return underlyingType;
    }
}
