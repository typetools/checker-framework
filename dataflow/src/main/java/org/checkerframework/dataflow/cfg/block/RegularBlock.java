package org.checkerframework.dataflow.cfg.block;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Pure;

/**
 * A regular basic block that contains a sequence of {@link Node}s.
 *
 * <p>The following invariant holds.
 *
 * <pre>
 * forall n in getContents() :: n.getBlock() == this
 * </pre>
 */
public interface RegularBlock extends SingleSuccessorBlock {

    /** @return the unmodifiable sequence of {@link Node}s. */
    @Pure
    List<Node> getContents();

    /** @return the regular successor block */
    @Pure
    @Nullable Block getRegularSuccessor();

    /** Is this block empty (i.e., does it not contain any contents). */
    @Pure
    boolean isEmpty();
}
