package org.checkerframework.dataflow.cfg.block;

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

    @Override
    public String toString() {
        return "SpecialBlock(" + specialType + ")";
    }
}
