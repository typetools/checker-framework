package dataflow.cfg.block;

/**
 * A basic block that has at exactly one non-exceptional successor.
 *
 * @author Stefan Heule
 *
 */
public interface SingleSuccessorBlock extends Block {

    /**
     * @return The non-exceptional successor block, or {@code null} if there is
     *         no successor.
     */
    /*@Nullable*/ Block getSuccessor();

}
