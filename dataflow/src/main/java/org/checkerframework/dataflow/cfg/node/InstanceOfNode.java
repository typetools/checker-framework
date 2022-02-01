package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.InstanceOfTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TypesUtils;

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

    /** The node of the binding variable if one exists. */
    protected final @Nullable LocalVariableNode bindingVariable;

    /** For Types.isSameType. */
    protected final Types types;

    /**
     * Create an InstanceOfNode.
     *
     * @param tree instanceof tree
     * @param operand the expression in the instanceof tree
     * @param refType the type in the instanceof
     * @param types types util
     */
    public InstanceOfNode(InstanceOfTree tree, Node operand, TypeMirror refType, Types types) {
        this(tree, operand, null, refType, types);
    }

    /**
     * Create an InstanceOfNode.
     *
     * @param tree instanceof tree
     * @param operand the expression in the instanceof tree
     * @param bindingVariable the binding variable or null if there is none
     * @param refType the type in the instanceof
     * @param types types util
     */
    public InstanceOfNode(
            InstanceOfTree tree,
            Node operand,
            @Nullable LocalVariableNode bindingVariable,
            TypeMirror refType,
            Types types) {
        super(types.getPrimitiveType(TypeKind.BOOLEAN));
        this.tree = tree;
        this.operand = operand;
        this.refType = refType;
        this.types = types;
        this.bindingVariable = bindingVariable;
    }

    public Node getOperand() {
        return operand;
    }

    /**
     * Returns the binding variable for this instanceof, or null if one does not exist.
     *
     * @return the binding variable for this instanceof, or null if one does not exist
     */
    public @Nullable LocalVariableNode getBindingVariable() {
        return bindingVariable;
    }

    /**
     * The reference type being tested against.
     *
     * @return the reference type
     */
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
        return "("
                + getOperand()
                + " instanceof "
                + TypesUtils.simpleTypeName(getRefType())
                + (bindingVariable == null ? "" : " " + getBindingVariable())
                + ")";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof InstanceOfNode)) {
            return false;
        }
        InstanceOfNode other = (InstanceOfNode) obj;
        // TODO: TypeMirror.equals may be too restrictive.
        // Check whether Types.isSameType is the better comparison.
        return getOperand().equals(other.getOperand())
                && types.isSameType(getRefType(), other.getRefType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(InstanceOfNode.class, getOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(getOperand());
    }
}
