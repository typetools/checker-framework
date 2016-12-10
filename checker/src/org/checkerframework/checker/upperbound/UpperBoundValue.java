package org.checkerframework.checker.upperbound;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;

public class UpperBoundValue extends CFAbstractValue<UpperBoundValue> {

    public UpperBoundValue(
            CFAbstractAnalysis<UpperBoundValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror type) {
        super(analysis, annotations, type);
    }
}
