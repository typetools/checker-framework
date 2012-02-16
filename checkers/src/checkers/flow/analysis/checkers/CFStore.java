package checkers.flow.analysis.checkers;

/**
 * The default store used in the Checker Framework.
 * 
 * @author Stefan Heule
 * 
 */
public class CFStore extends CFAbstractStore<CFValue, CFStore> {

    public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis) {
        super(analysis);
    }

    public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis,
            CFAbstractStore<CFValue, CFStore> other) {
        super(other);
    }

}
