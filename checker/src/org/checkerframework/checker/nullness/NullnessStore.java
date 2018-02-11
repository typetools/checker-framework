package org.checkerframework.checker.nullness;

import org.checkerframework.checker.initialization.InitializationStore;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

/**
 * Behaves like {@link InitializationStore}, but additionally tracks whether {@link PolyNull} is
 * known to be {@link Nullable}.
 */
public class NullnessStore extends InitializationStore<NullnessValue, NullnessStore> {

    protected boolean isPolyNullNull;

    public NullnessStore(
            CFAbstractAnalysis<NullnessValue, NullnessStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        isPolyNullNull = false;
    }

    public NullnessStore(NullnessStore s) {
        super(s);
        isPolyNullNull = s.isPolyNullNull;
    }

    @Override
    public NullnessStore leastUpperBound(NullnessStore other) {
        NullnessStore lub = super.leastUpperBound(other);
        if (isPolyNullNull == other.isPolyNullNull) {
            lub.isPolyNullNull = isPolyNullNull;
        } else {
            lub.isPolyNullNull = false;
        }
        return lub;
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<NullnessValue, NullnessStore> o) {
        if (!(o instanceof InitializationStore)) {
            return false;
        }
        NullnessStore other = (NullnessStore) o;
        if (other.isPolyNullNull != isPolyNullNull) {
            return false;
        }
        return super.supersetOf(other);
    }

    @Override
    protected void internalVisualize(CFGVisualizer<NullnessValue, NullnessStore, ?> viz) {
        super.internalVisualize(viz);
        viz.visualizeStoreKeyVal("isPolyNonNull", isPolyNullNull);
    }

    public boolean isPolyNullNull() {
        return isPolyNullNull;
    }

    public void setPolyNullNull(boolean isPolyNullNull) {
        this.isPolyNullNull = isPolyNullNull;
    }
}
