package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A node for the widening primitive conversion operation. See JLS 5.1.2 for the definition of
 * widening primitive conversion.
 *
 * <p>A {@link WideningConversionNode} does not correspond to any tree node in the parsed AST. It is
 * introduced when a value of some primitive type appears in a context that requires a different
 * primitive with more bits of precision.
 */
public class WideningConversionNode extends Node {

    protected final Tree tree;
    protected final Node operand;

    public WideningConversionNode(Tree tree, Node operand, TypeMirror type) {
        super(type);
        assert TypesUtils.isPrimitive(type) : "non-primitive type in widening conversion";
        this.tree = tree;
        this.operand = operand;
    }

    public Node getOperand() {
        return operand;
    }

    @Override
    public TypeMirror getType() {
        return type;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitWideningConversion(this, p);
    }

    @Override
    public String toString() {
        return "WideningConversion(" + getOperand() + ", " + type + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WideningConversionNode)) {
            return false;
        }
        WideningConversionNode other = (WideningConversionNode) obj;
        return getOperand().equals(other.getOperand())
                && TypesUtils.areSamePrimitiveTypes(getType(), other.getType());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getOperand());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(getOperand());
    }
}
