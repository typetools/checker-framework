package org.checkerframework.dataflow.livevariable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * A LiveVarNode contains a CFG node, which can only be a LocalVariableNode or FieldAccessNode. It
 * is used to represent the estimate of live variables at certain CFG block during dataflow
 * analysis. We override `.equals` in this class to compare nodes by value equality rather than
 * reference equality. We want two different nodes with the same value (that is, two nodes refer to
 * the same live variable in the program) to be regarded as the same.
 */
public class LiveVarNode {

    /**
     * A live variable is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.LocalVariableNode} or {@link
     * org.checkerframework.dataflow.cfg.node.FieldAccessNode}.
     */
    protected final Node liveVariable;

    /**
     * Create a new live variable.
     *
     * @param n a node
     */
    public LiveVarNode(Node n) {
        assert n instanceof FieldAccessNode || n instanceof LocalVariableNode;
        this.liveVariable = n;
    }

    @Override
    public int hashCode() {
        return this.liveVariable.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof LiveVarNode)) {
            return false;
        }
        LiveVarNode other = (LiveVarNode) obj;
        // We use `.equals` instead of `==` here to compare value equality.
        return this.liveVariable.equals(other.liveVariable);
    }

    @Override
    public String toString() {
        return this.liveVariable.toString();
    }
}
