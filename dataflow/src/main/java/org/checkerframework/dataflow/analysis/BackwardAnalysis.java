package org.checkerframework.dataflow.analysis;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This interface defines a backward analysis, given a control flow graph and a backward transfer
 * function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the backward transfer function type that is used to approximate runtime behavior
 */
public interface BackwardAnalysis<
                V extends AbstractValue<V>,
                S extends Store<S>,
                T extends BackwardTransferFunction<V, S>>
        extends Analysis<V, S, T> {

    /**
     * Get the output store at the entry block of a given control flow graph. For a backward
     * analysis, the output store contains the analyzed flow information from the exit block to the
     * entry block.
     *
     * @return the output store at the entry block of a given control flow graph
     */
    @Nullable S getEntryStore();
}
