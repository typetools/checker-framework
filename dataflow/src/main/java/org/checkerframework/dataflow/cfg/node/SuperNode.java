package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a reference to 'super'.
 *
 * <pre>
 *   <em>super</em>
 * </pre>
 */
public class SuperNode extends Node {

    protected final Tree tree;

    public SuperNode(Tree t) {
        super(TreeUtils.typeOf(t));
        assert t instanceof IdentifierTree && ((IdentifierTree) t).getName().contentEquals("super");
        tree = t;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitSuper(this, p);
    }

    public String getName() {
        return "super";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SuperNode;
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getName());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
