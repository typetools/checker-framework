package checkers.flow.analysis.checkers;

public class CFStore extends CFAbstractStore<CFValue, CFStore> {

	public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis) {
		super(analysis);
	}

	public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis,
			CFAbstractStore<CFValue, CFStore> other) {
		super(analysis, other);
	}

}
