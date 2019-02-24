package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * A node for the instanceof operator:
 *
 * <p><em>x</em> instanceof <em>Point</em>
 */
public class InstanceOfNode extends Node {

    /** The value being tested. */
    protected final Node operand;

    /** The reference type being tested against. */
    protected final TypeMirror refType;

    /** The tree associated with this node. */
    protected final InstanceOfTree tree;

    public InstanceOfNode(Tree tree, Node operand, TypeMirror refType, Types types) {
        super(types.getPrimitiveType(TypeKind.BOOLEAN));
        assert tree.getKind() == Tree.Kind.INSTANCE_OF;
        this.tree = (InstanceOfTree) tree;
        this.operand = operand;
        this.refType = refType;
    }

    public Node getOperand() {
        return operand;
    }

    @Override
    public TypeMirror getType() {
        return type;
    }

    public TypeMirror getRefType() {
        return refType;
    }

    @Override
    public InstanceOfTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitInstanceOf(this, p);
    }

    @Override
    public String toString() {
        return "(" + getOperand() + " instanceof " + getRefType() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InstanceOfNode)) {
            return false;
        }
        InstanceOfNode other = (InstanceOfNode) obj;
        // TODO: TypeMirror.equals may be too restrictive.
        // Check whether Types.isSameType is the better comparison.
        return getOperand().equals(other.getOperand()) && getRefType().equals(other.getRefType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(getOperand());
    }
}
