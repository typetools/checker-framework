package checkers.nonnull;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.initialization.InitializationStore;

public class NonNullStore extends InitializationStore<NonNullStore> {

    protected boolean isPolyNullNull;

    public NonNullStore(CFAbstractAnalysis<CFValue, NonNullStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        isPolyNullNull = false;
    }

    public NonNullStore(NonNullStore s) {
        super(s);
        isPolyNullNull = s.isPolyNullNull;
    }

    @Override
    public NonNullStore leastUpperBound(NonNullStore other) {
        NonNullStore lub = super.leastUpperBound(other);
        if (isPolyNullNull == other.isPolyNullNull) {
            lub.isPolyNullNull = isPolyNullNull;
        } else {
            lub.isPolyNullNull = false;
        }
        return lub;
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<CFValue, NonNullStore> o) {
        if (!(o instanceof InitializationStore)) {
            return false;
        }
        NonNullStore other = (NonNullStore) o;
        if (other.isPolyNullNull != isPolyNullNull) {
            return false;
        }
        return super.supersetOf(other);
    }

    @Override
    protected void internalDotOutput(StringBuilder result) {
        super.internalDotOutput(result);
        result.append("  isPolyNonNull = " + isPolyNullNull + "\\n");
    }

    public boolean isPolyNullNull() {
        return isPolyNullNull;
    }

    public void setPolyNullNull(boolean isPolyNullNull) {
        this.isPolyNullNull = isPolyNullNull;
    }
}
