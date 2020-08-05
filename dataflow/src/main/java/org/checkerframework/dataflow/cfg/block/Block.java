package org.checkerframework.dataflow.cfg.block;

import java.util.Set;

/** Represents a basic block in a control flow graph. */
public interface Block {

    /** The types of basic blocks. */
    public static enum BlockType {

        /** A regular basic block. */
        REGULAR_BLOCK,

        /** A conditional basic block. */
        CONDITIONAL_BLOCK,

        /** A special basic block. */
        SPECIAL_BLOCK,

        /** A basic block that can throw an exception. */
        EXCEPTION_BLOCK,
    }

    /**
     * Returns the type of this basic block.
     *
     * @return the type of this basic block
     */
    BlockType getType();

    /**
     * Returns the unique identifier of this block.
     *
     * @return the unique identifier of this block
     */
    long getId();

    /**
     * Returns the predecessors of this basic block.
     *
     * @return the predecessors of this basic block
     */
    Set<Block> getPredecessors();

    /**
     * Returns the successors of this basic block.
     *
     * @return the successors of this basic block
     */
    Set<Block> getSuccessors();
}
