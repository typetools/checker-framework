package org.checkerframework.dataflow.cfg.builder;

import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;
import org.checkerframework.dataflow.cfg.node.Node;

/** An extended node of type {@code EXCEPTION_NODE}. */
class NodeWithExceptionsHolder extends ExtendedNode {

    /** The node to hold. */
    protected final Node node;

    /**
     * Map from exception type to labels of successors that may be reached as a result of that
     * exception.
     */
    protected final Map<TypeMirror, Set<Label>> exceptions;

    /**
     * Construct a NodeWithExceptionsHolder for the given node and exceptions.
     *
     * @param node the node to hold
     * @param exceptions the exceptions to hold
     */
    public NodeWithExceptionsHolder(Node node, Map<TypeMirror, Set<Label>> exceptions) {
        super(ExtendedNodeType.EXCEPTION_NODE);
        this.node = node;
        this.exceptions = exceptions;
    }

    /**
     * Get the exceptions for the node.
     *
     * @return exceptions for the node
     */
    public Map<TypeMirror, Set<Label>> getExceptions() {
        return exceptions;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "NodeWithExceptionsHolder(" + node + ")";
    }

    @Override
    public String toStringDebug() {
        return "NodeWithExceptionsHolder(" + node.toStringDebug() + ")";
    }
}
