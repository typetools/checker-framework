package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class LowerBoundAnalysis extends CFAbstractAnalysis<CFValue, CFStore, LowerBoundTransfer> {
    public LowerBoundAnalysis(BaseTypeChecker checker, LowerBoundAnnotatedTypeFactory factory,
                              List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public LowerBoundTransfer createTransferFunction() {
        return new LowerBoundTransfer(this);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return new CFValue(this, type);
    }

    @Override
    public CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    public CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }
}
