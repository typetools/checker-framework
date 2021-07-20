package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.Collections;
import java.util.List;

/** The implementation of a {@link SpecialBlock}. */
public class SpecialBlockImpl extends SingleSuccessorBlockImpl implements SpecialBlock {

    /** The type of this special basic block. */
    protected final SpecialBlockType specialType;

    public SpecialBlockImpl(SpecialBlockType type) {
        super(BlockType.SPECIAL_BLOCK);
        this.specialType = type;
    }

    @Override
    public SpecialBlockType getSpecialType() {
        return specialType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns an empty list.
     */
    @Override
    public List<Node> getNodes() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable Node getLastNode() {
        return null;
    }

    @Override
    public String toString() {
        return "SpecialBlock(" + specialType + ")";
    }
}
