package org.checkerframework.checker.lowerbound;

import org.checkerframework.framework.flow.CFAbstractAnalysis;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class LowerBoundAnalysis extends CFAbstractAnalysis<LowerBoundValue, LowerBoundStore, LowerBoundTransfer> {
    public LowerBoundAnalysis(BaseTypeChecker checker, LowerBoundAnnotatedTypeFactory factory,
			      List<Pair<VariableElement, LowerBoundValue>> fieldValues) {
	super(checker, factory, fieldValues);
    }

    @Override
    public LowerBoundTransfer createTransferFunction() {
	return new LowerBoundTransfer(this);
    }

    @Override
    public LowerBoundValue createAbstractValue(AnnotatedTypeMirror type) {
	return new LowerBoundValue(this, type);
    }

    @Override
    public LowerBoundStore createCopiedStore(LowerBoundStore s) {
	return new LowerBoundStore(s);
    }

    @Override
    public LowerBoundStore createEmptyStore(boolean sequentialSemantics) {
        return new LowerBoundStore(this, sequentialSemantics);
    }
}
