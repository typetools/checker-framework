package org.checkerframework.checker.nullness;

import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.InitializationStore;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.plumelib.util.UniqueId;

/**
 * Behaves like {@link InitializationStore}, but additionally tracks whether {@link PolyNull} is
 * known to be {@link Nullable}.
 */
public class NullnessStore extends InitializationStore<NullnessValue, NullnessStore>
        implements UniqueId {

    /** True if, at this point, {@link PolyNull} is known to be {@link Nullable}. */
    protected boolean isPolyNullNull;

    /** The unique ID for the next-created object. */
    static final AtomicLong nextUid = new AtomicLong(0);
    /** The unique ID of this object. */
    final long uid = nextUid.getAndIncrement();
    /**
     * Returns the unique ID of this object.
     *
     * @return the unique ID of this object.
     */
    @Override
    public long getUid() {
        return uid;
    }

    /**
     * Create a NullnessStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics (i.e., assume
     *     that only one thread is running at all times)?
     */
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
    protected String internalVisualize(CFGVisualizer<NullnessValue, NullnessStore, ?> viz) {
        return super.internalVisualize(viz)
                + viz.getSeparator()
                + viz.visualizeStoreKeyVal("isPolyNonNull", isPolyNullNull);
    }

    public boolean isPolyNullNull() {
        return isPolyNullNull;
    }

    public void setPolyNullNull(boolean isPolyNullNull) {
        this.isPolyNullNull = isPolyNullNull;
    }
}
