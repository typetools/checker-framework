package org.checkerframework.dataflow.cfg.block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Base class of the {@link Block} implementation hierarchy. */
public abstract class BlockImpl implements Block {

    /** A unique ID for this node. */
    protected final long id = BlockImpl.uniqueID();

    /** The last ID that has already been used. */
    protected static long lastId = 0;

    /** The type of this basic block. */
    protected final BlockType type;

    /** The set of predecessors. */
    protected final Set<BlockImpl> predecessors;

    /**
     * Returns a fresh identifier.
     *
     * @return a fresh identifier
     */
    private static long uniqueID() {
        return lastId++;
    }

    protected BlockImpl(BlockType type) {
        this.type = type;
        this.predecessors = new HashSet<>();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public BlockType getType() {
        return type;
    }

    @Override
    public Set<BlockImpl> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    public void addPredecessor(BlockImpl pred) {
        predecessors.add(pred);
    }

    public void removePredecessor(BlockImpl pred) {
        predecessors.remove(pred);
    }
}
