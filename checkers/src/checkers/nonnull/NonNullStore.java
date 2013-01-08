package checkers.nonnull;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.initialization.InitializationStore;

public class NonNullStore extends InitializationStore<NonNullStore> {

    public NonNullStore(CFAbstractAnalysis<CFValue, NonNullStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public NonNullStore(NonNullStore s) {
        super(s);
    }

}
