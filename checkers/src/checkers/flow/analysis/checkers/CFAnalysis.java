package checkers.flow.analysis.checkers;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeFactory;

public class CFAnalysis extends
		CFAbstractAnalysis<CFValue, CFStore, CFTransfer> {

	public CFAnalysis(AnnotatedTypeFactory factory) {
		super(factory);
	}

	@Override
	protected CFTransfer createTransferFunction() {
		return new CFTransfer(this);
	}

	@Override
	protected CFStore createEmptyStore() {
		return new CFStore(this);
	}

	@Override
	protected CFStore createCopiedStore(CFStore s) {
		return new CFStore(this, s);
	}

	@Override
	protected CFValue createAbstractValue(Set<AnnotationMirror> annotations) {
		return new CFValue(this, annotations);
	}

}
