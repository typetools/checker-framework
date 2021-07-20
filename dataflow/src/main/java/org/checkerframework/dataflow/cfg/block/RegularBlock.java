package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Pure;

import java.util.List;

/** A regular basic block that contains a sequence of {@link Node}s. */
public interface RegularBlock extends SingleSuccessorBlock {

    /**
     * Returns the unmodifiable sequence of {@link Node}s.
     *
     * @return the unmodifiable sequence of {@link Node}s
     * @deprecated use {@link #getNodes} instead
     */
    @Deprecated // use getNodes instead
    @Pure
    List<Node> getContents();

    /**
     * Returns the regular successor block.
     *
     * @return the regular successor block
     */
    @Pure
    @Nullable Block getRegularSuccessor();

    /** Is this block empty (i.e., does it not contain any contents). */
    @Pure
    boolean isEmpty();
}
