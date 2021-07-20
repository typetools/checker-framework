package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * A node for the cast operator:
 *
 * <p>(<em>Point</em>) <em>x</em>
 */
public class TypeCastNode extends Node {

    protected final Tree tree;
    protected final Node operand;

    /** For Types.isSameType. */
    protected final Types types;

    public TypeCastNode(Tree tree, Node operand, TypeMirror type, Types types) {
        super(type);
        this.tree = tree;
        this.operand = operand;
        this.types = types;
    }

    public Node getOperand() {
        return operand;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitTypeCast(this, p);
    }

    @Override
    public String toString() {
        return "(" + getType() + ")" + getOperand();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TypeCastNode)) {
            return false;
        }
        TypeCastNode other = (TypeCastNode) obj;
        return getOperand().equals(other.getOperand())
                && types.isSameType(getType(), other.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(getOperand());
    }
}
