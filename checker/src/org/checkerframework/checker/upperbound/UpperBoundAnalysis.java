package org.checkerframework.checker.upperbound;

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

public class UpperBoundAnalysis
        extends CFAbstractAnalysis<UpperBoundValue, UpperBoundStore, UpperBoundTransfer> {
    UpperBoundAnnotatedTypeFactory atypeFactory;

    public UpperBoundAnalysis(
            BaseTypeChecker checker,
            UpperBoundAnnotatedTypeFactory factory,
            List<Pair<VariableElement, UpperBoundValue>> fieldValues) {
        super(checker, factory, fieldValues);
        this.atypeFactory = (UpperBoundAnnotatedTypeFactory) super.atypeFactory;
    }

    //overrides the superclass method to return our transfers
    @Override
    public UpperBoundTransfer createTransferFunction() {
        return new UpperBoundTransfer(this);
    }

    @Override
    public @Nullable UpperBoundValue createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, qualifierHierarchy)) {
            return null;
        }
        return new UpperBoundValue(this, annotations, underlyingType);
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
