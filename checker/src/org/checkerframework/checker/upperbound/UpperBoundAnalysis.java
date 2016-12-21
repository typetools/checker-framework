package org.checkerframework.checker.upperbound;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.Pair;

/** Analysis class using {@link UpperBoundStore} and {@link UpperBoundTransfer}. */
public class UpperBoundAnalysis
        extends CFAbstractAnalysis<CFValue, UpperBoundStore, UpperBoundTransfer> {

    public UpperBoundAnalysis(
            BaseTypeChecker checker,
            UpperBoundAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public UpperBoundTransfer createTransferFunction() {
        return new UpperBoundTransfer(this);
    }

    @Override
    public CFValue createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
        return defaultCreateAbstractValue(this, annotations, underlyingType);
    }

    @Override
    public UpperBoundStore createCopiedStore(UpperBoundStore s) {
        return new UpperBoundStore(s);
    }

    @Override
    public UpperBoundStore createEmptyStore(boolean sequentialSemantics) {
        return new UpperBoundStore(this, sequentialSemantics);
    }
}
