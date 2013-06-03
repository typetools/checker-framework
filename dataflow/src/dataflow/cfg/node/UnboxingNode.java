package dataflow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.type.TypeMirror;

import dataflow.util.HashCodeUtils;

import com.sun.source.tree.Tree;

/**
 * A node for the unboxing conversion operation. See JLS 5.1.8 for the
 * definition of unboxing.
 *
 * An {@link UnboxingNode} does not correspond to any tree node in the parsed
 * AST. It is introduced when a value of reference type appears in a context
 * that requires a primitive type.
 *
 * Unboxing a null value throws a {@link NullPointerException} while unboxing
 * any other value succeeds.
 *
 * @author Stefan Heule
 * @author Charlie Garrett
 *
 */
public class UnboxingNode extends Node {

    protected Tree tree;
    protected Node operand;

    public UnboxingNode(Tree tree, Node operand, TypeMirror type) {
        super(type);
        this.tree = tree;
        this.operand = operand;
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
        return visitor.visitUnboxing(this, p);
    }

    @Override
    public String toString() {
        return "Unboxing(" + getOperand() + ", " + type + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof UnboxingNode)) {
            return false;
        }
        UnboxingNode other = (UnboxingNode) obj;
        return getOperand().equals(other.getOperand());
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
