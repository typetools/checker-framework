package checkers.flow.analysis.checkers;

public class CFTransfer extends
		CFAbstractTransfer<CFValue, CFStore, CFTransfer> {

	public CFTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
		super(analysis);
	}

}
