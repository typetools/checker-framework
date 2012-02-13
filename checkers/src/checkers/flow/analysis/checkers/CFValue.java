package checkers.flow.analysis.checkers;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

public class CFValue extends CFAbstractValue<CFValue> {

	public CFValue(CFAbstractAnalysis<CFValue, ?, ?> analysis,
			Set<AnnotationMirror> annotations) {
		super(analysis, annotations);
	}

}
