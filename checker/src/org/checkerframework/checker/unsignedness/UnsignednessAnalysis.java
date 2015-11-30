package org.checkerframework.checker.unsignedness;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

public class UnsignednessAnalysis extends
		CFAbstractAnalysis<CFValue, CFStore, UnsignednessTransfer> {

	public UnsignednessAnalysis(BaseTypeChecker checker,
			UnsignednessTypeFactory factory,
			List<Pair<VariableElement, CFValue>> fieldValues) {
		super(checker, factory, fieldValues);
	}

	@Override
	public UnsignednessTransfer createTransferFunction() {
		return new UnsignednessTransfer(this);
	}

	@Override
	public CFStore createEmptyStore(boolean sequentialSemantics) {
		return new CFStore(this, sequentialSemantics);
	}

	@Override
	public CFStore createCopiedStore(CFStore s) {
		return new CFStore(this, s);
	}

	@Override
	public CFValue createAbstractValue(AnnotatedTypeMirror type) {
		return defaultCreateAbstractValue(this, type);
	}
}
