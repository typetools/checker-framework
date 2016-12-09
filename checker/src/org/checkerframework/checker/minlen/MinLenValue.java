package org.checkerframework.checker.minlen;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;

public class MinLenValue extends CFAbstractValue<MinLenValue> {

    public MinLenValue(
            CFAbstractAnalysis<MinLenValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror type) {
        super(analysis, annotations, type);
    }
}
