package org.checkerframework.dataflow.reachingdef;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;

/**
 * A ReachingDefinitionNode contains a CFG node, which can only be a AssignmentNode. It is used to
 * represent the estimate of a reaching definition at certain CFG block during dataflow analysis. We
 * override `.equals` in this class to compare Nodes by value equality rather than reference
 * equality. We want two different nodes with the same values (that is, the two nodes refer to the
 * same reaching definition in the program) to be regarded as the same here.
 */
public class ReachingDefinitionNode {

    /**
     * A reaching definition is represented by a node, which can only be a {@link
     * org.checkerframework.dataflow.cfg.node.AssignmentNode}.
     */
    protected final AssignmentNode def;

    /**
     * Create a new reaching definition.
     *
     * @param n an assignment node
     */
    public ReachingDefinitionNode(AssignmentNode n) {
        this.def = n;
    }

    @Override
    public int hashCode() {
        return this.def.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReachingDefinitionNode)) {
            return false;
        }
        ReachingDefinitionNode other = (ReachingDefinitionNode) obj;
        // We use `.equals` instead of `==` here to compare value equality.
        return this.def.equals(other.def);
    }

    @Override
    public String toString() {
        return this.def.toString();
    }
}
