package org.checkerframework.checker.minlen;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.Pair;

// subclasses the base analysis to use our Transfers instead of the deafaults

public class MinLenAnalysis extends CFAbstractAnalysis<MinLenValue, MinLenStore, MinLenTransfer> {
    MinLenAnnotatedTypeFactory atypeFactory;

    public MinLenAnalysis(
            BaseTypeChecker checker,
            MinLenAnnotatedTypeFactory factory,
            List<Pair<VariableElement, MinLenValue>> fieldValues) {
        super(checker, factory, fieldValues);
        this.atypeFactory = (MinLenAnnotatedTypeFactory) super.atypeFactory;
    }

    //overrides the superclass method to return our transfers
    @Override
    public MinLenTransfer createTransferFunction() {
        return new MinLenTransfer(this);
    }

    @Override
    public @Nullable MinLenValue createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, qualifierHierarchy)) {
            return null;
        }
        return new MinLenValue(this, annotations, underlyingType);
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
