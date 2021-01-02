package org.checkerframework.dataflow.cfg.builder;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store.FlowRule;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlockImpl;

/* --------------------------------------------------------- */
/* Phase Two */
/* --------------------------------------------------------- */

/** Represents a missing edge that will be added later. */
class MissingEdge {
    /** The source of the edge. */
    final SingleSuccessorBlockImpl source;
    /** The index (target?) of the edge. Null means go to exceptional exit. */
    final @Nullable Integer index;
    /** The cause exception type, for an exceptional edge; otherwise null. */
    final @Nullable TypeMirror cause;

    /** The flow rule for this edge. */
    final @Nullable FlowRule flowRule;

    /**
     * Create a new MissingEdge.
     *
     * @param source the source of the edge
     * @param index the index (target?) of the edge
     */
    public MissingEdge(SingleSuccessorBlockImpl source, int index) {
        this(source, index, null, FlowRule.EACH_TO_EACH);
    }

    /**
     * Create a new MissingEdge.
     *
     * @param source the source of the edge
     * @param index the index (target?) of the edge
     * @param flowRule the flow rule for this edge
     */
    public MissingEdge(SingleSuccessorBlockImpl source, int index, FlowRule flowRule) {
        this(source, index, null, flowRule);
    }

    /**
     * Create a new MissingEdge.
     *
     * @param source the source of the edge
     * @param index the index (target?) of the edge; null means go to exceptional exit
     * @param cause the cause exception type, for an exceptional edge; otherwise null
     */
    public MissingEdge(
            SingleSuccessorBlockImpl source, @Nullable Integer index, @Nullable TypeMirror cause) {
        this(source, index, cause, FlowRule.EACH_TO_EACH);
    }

    /**
     * Create a new MissingEdge.
     *
     * @param source the source of the edge
     * @param index the index (target?) of the edge; null means go to exceptional exit
     * @param cause the cause exception type, for an exceptional edge; otherwise null
     * @param flowRule the flow rule for this edge
     */
    public MissingEdge(
            SingleSuccessorBlockImpl source,
            @Nullable Integer index,
            @Nullable TypeMirror cause,
            FlowRule flowRule) {
        assert (index != null) || (cause != null);
        this.source = source;
        this.index = index;
        this.cause = cause;
        this.flowRule = flowRule;
    }

    @Override
    public String toString() {
        return "MissingEdge(" + source + ", " + index + ", " + cause + ")";
    }
}
