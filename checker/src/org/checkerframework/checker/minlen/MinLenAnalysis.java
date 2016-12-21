package org.checkerframework.checker.minlen;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.Pair;

/** Analysis class using {@link MinLenStore} and {@link MinLenTransfer}. */
public class MinLenAnalysis extends CFAbstractAnalysis<CFValue, MinLenStore, MinLenTransfer> {
    MinLenAnnotatedTypeFactory atypeFactory;

    public MinLenAnalysis(
            BaseTypeChecker checker,
            MinLenAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
        this.atypeFactory = (MinLenAnnotatedTypeFactory) super.atypeFactory;
    }

    @Override
    public MinLenTransfer createTransferFunction() {
        return new MinLenTransfer(this);
    }

    @Override
    public CFValue createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
        return defaultCreateAbstractValue(this, annotations, underlyingType);
    }

    @Override
    public MinLenStore createCopiedStore(MinLenStore s) {
        return new MinLenStore(s);
    }

    @Override
    public MinLenStore createEmptyStore(boolean sequentialSemantics) {
        return new MinLenStore(this, sequentialSemantics);
    }
}
