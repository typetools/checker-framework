package org.checkerframework.checker.index.upperbound;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.Pair;

public class UpperBoundAnalysis
        extends CFAbstractAnalysis<CFValue, UpperBoundStore, UpperBoundTransfer> {
    public UpperBoundAnalysis(
            BaseTypeChecker checker,
            UpperBoundAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public UpperBoundStore createEmptyStore(boolean sequentialSemantics) {
        return new UpperBoundStore(this, sequentialSemantics);
    }

    @Override
    public UpperBoundStore createCopiedStore(UpperBoundStore s) {
        return new UpperBoundStore(this, s);
    }

    @Override
    public CFValue createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
        return defaultCreateAbstractValue(this, annotations, underlyingType);
    }

    public SourceChecker getChecker() {
        return checker;
    }
}
