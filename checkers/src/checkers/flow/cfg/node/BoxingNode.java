package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.type.TypeMirror;

import javacutils.InternalUtils;

import checkers.flow.util.HashCodeUtils;
import checkers.nullness.quals.NonNull;

import com.sun.source.tree.Tree;

/**
 * A node for the boxing conversion operation. See JLS 5.1.7 for the definition
 * of boxing.
 * 
 * A {@link BoxingNode} does not correspond to any tree node in the parsed AST.
 * It is introduced when a value of primitive type appears in a context that
 * requires a reference.
 * 
 * Boxing can fail with an {@link OutOfMemoryError}, but that is not something
 * we consider in our dataflow analyses. Boxing of primitive types always yields
 * a {@link NonNull} reference, however, boxing of null values yields null.
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class BoxingNode extends Node {

    protected Tree tree;
    protected Node operand;

    public BoxingNode(Tree tree, Node operand, TypeMirror type) {
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
        return visitor.visitBoxing(this, p);
    }

    @Override
    public String toString() {
        return "Boxing(" + getOperand() + ", " + type + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BoxingNode)) {
            return false;
        }
        BoxingNode other = (BoxingNode) obj;
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
