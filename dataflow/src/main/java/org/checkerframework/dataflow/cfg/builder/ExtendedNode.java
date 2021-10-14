package org.checkerframework.dataflow.cfg.builder;

import org.checkerframework.dataflow.cfg.block.BlockImpl;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

/**
 * An extended node can be one of several things (depending on its {@code type}):
 *
 * <ul>
 *   <li><em>NODE</em>: {@link CFGBuilder.NodeHolder}. An extended node of this type is just a
 *       wrapper for a {@link Node} (that cannot throw exceptions).
 *   <li><em>EXCEPTION_NODE</em>: {@link CFGBuilder.NodeWithExceptionsHolder}. A wrapper for a
 *       {@link Node} which can throw exceptions. It contains a label for every possible exception
 *       type the node might throw.
 *   <li><em>UNCONDITIONAL_JUMP</em>: {@link CFGBuilder.UnconditionalJump}. An unconditional jump to
 *       a label.
 *   <li><em>TWO_TARGET_CONDITIONAL_JUMP</em>: {@link CFGBuilder.ConditionalJump}. A conditional
 *       jump with two targets for both the 'then' and 'else' branch.
 * </ul>
 */
@SuppressWarnings("nullness") // TODO
/*package-private*/ abstract class ExtendedNode {

    /** The basic block this extended node belongs to (as determined in phase two). */
    protected BlockImpl block;

    /** Type of this node. */
    protected final ExtendedNodeType type;

    /** Does this node terminate the execution? (e.g., "System.exit()") */
    protected boolean terminatesExecution = false;

    /**
     * Create a new ExtendedNode.
     *
     * @param type the type of this node
     */
    protected ExtendedNode(ExtendedNodeType type) {
        this.type = type;
    }

    /** Extended node types (description see above). */
    public enum ExtendedNodeType {
        NODE,
        EXCEPTION_NODE,
        UNCONDITIONAL_JUMP,
        CONDITIONAL_JUMP
    }

    public ExtendedNodeType getType() {
        return type;
    }

    public boolean getTerminatesExecution() {
        return terminatesExecution;
    }

    public void setTerminatesExecution(boolean terminatesExecution) {
        this.terminatesExecution = terminatesExecution;
    }

    /**
     * Returns the node contained in this extended node (only applicable if the type is {@code NODE}
     * or {@code EXCEPTION_NODE}).
     *
     * @return the node contained in this extended node (only applicable if the type is {@code NODE}
     *     or {@code EXCEPTION_NODE})
     */
    public Node getNode() {
        throw new BugInCF("Do not call");
    }

    /**
     * Returns the label associated with this extended node (only applicable if type is {@link
     * ExtendedNodeType#CONDITIONAL_JUMP} or {@link ExtendedNodeType#UNCONDITIONAL_JUMP}).
     *
     * @return the label associated with this extended node (only applicable if type is {@link
     *     ExtendedNodeType#CONDITIONAL_JUMP} or {@link ExtendedNodeType#UNCONDITIONAL_JUMP})
     */
    public Label getLabel() {
        throw new BugInCF("Do not call");
    }

    public BlockImpl getBlock() {
        return block;
    }

    public void setBlock(BlockImpl b) {
        this.block = b;
    }

    @Override
    public String toString() {
        throw new BugInCF("DO NOT CALL ExtendedNode.toString(). Write your own.");
    }

    /**
     * Returns a verbose string representation of this, useful for debugging.
     *
     * @return a string representation of this
     */
    public abstract String toStringDebug();
}
