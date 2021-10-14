package org.checkerframework.dataflow.cfg.block;

/**
 * Represents a special basic block; i.e., one of the following:
 *
 * <ul>
 *   <li>Entry block of a method.
 *   <li>Regular exit block of a method.
 *   <li>Exceptional exit block of a method.
 * </ul>
 */
public interface SpecialBlock extends SingleSuccessorBlock {

    /** The types of special basic blocks. */
    public enum SpecialBlockType {

        /** The entry block of a method. */
        ENTRY,

        /** The exit block of a method. */
        EXIT,

        /** A special exit block of a method for exceptional termination. */
        EXCEPTIONAL_EXIT,
    }

    /**
     * Returns the type of this special basic block.
     *
     * @return the type of this special basic block
     */
    SpecialBlockType getSpecialType();
}
