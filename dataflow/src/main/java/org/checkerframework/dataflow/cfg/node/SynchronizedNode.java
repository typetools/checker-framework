package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.SynchronizedTree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;

/**
 * This represents the start and end of synchronized code block. If startOfBlock == true it is the
 * node preceding a synchronized code block. Otherwise it is the node immediately after a
 * synchronized code block.
 */
public class SynchronizedNode extends Node {

    protected final SynchronizedTree tree;
    protected final Node expression;
    protected final boolean startOfBlock;

    public SynchronizedNode(
            SynchronizedTree tree, Node expression, boolean startOfBlock, Types types) {
        super(types.getNoType(TypeKind.NONE));
        this.tree = tree;
        this.expression = expression;
        this.startOfBlock = startOfBlock;
    }

    @Override
    public SynchronizedTree getTree() {
        return tree;
    }

    public Node getExpression() {
        return expression;
    }

    public boolean getIsStartOfBlock() {
        return startOfBlock;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitSynchronized(this, p);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("synchronized (");
        sb.append(expression);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SynchronizedNode)) {
            return false;
        }
        SynchronizedNode other = (SynchronizedNode) obj;
        return Objects.equals(getTree(), other.getTree())
                && getExpression().equals(other.getExpression())
                && startOfBlock == other.startOfBlock;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree, startOfBlock, getExpression());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
