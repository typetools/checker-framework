package org.checkerframework.dataflow.cfg.node;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * A node for a reference to 'this', either implicit or explicit.
 *
 * <pre>
 *   <em>this</em>
 * </pre>
 */
public abstract class ThisLiteralNode extends Node {

    public ThisLiteralNode(TypeMirror type) {
        super(type);
    }

    public String getName() {
        return "this";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ThisLiteralNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
