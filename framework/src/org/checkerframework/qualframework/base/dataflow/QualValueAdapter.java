package org.checkerframework.qualframework.base.dataflow;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Created by mcarthur on 10/22/14.
 */
public class QualValueAdapter<Q> extends CFValue {

    public QualValueAdapter(CFAbstractAnalysis<CFValue, ?, ?> analysis,
            AnnotatedTypeMirror type) {
        super(analysis, type);
    }
}
