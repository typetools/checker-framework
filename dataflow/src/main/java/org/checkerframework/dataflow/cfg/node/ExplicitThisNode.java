package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;

import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a reference to 'this'.
 *
 * <pre>
 *   <em>this</em>
 * </pre>
 */
public class ExplicitThisNode extends ThisNode {

    protected final IdentifierTree tree;

    public ExplicitThisNode(IdentifierTree t) {
        super(TreeUtils.typeOf(t));
        assert t.getName().contentEquals("this");
        tree = t;
    }

    @Override
    public IdentifierTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitExplicitThis(this, p);
    }

    @Override
    public String toString() {
        return "this";
    }
}
