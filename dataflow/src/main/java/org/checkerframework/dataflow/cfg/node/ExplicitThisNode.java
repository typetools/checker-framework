package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a reference to 'this'.
 *
 * <pre>
 *   <em>this</em>
 * </pre>
 */
public class ExplicitThisNode extends ThisNode {

    protected final Tree tree;

    public ExplicitThisNode(Tree t) {
        super(TreeUtils.typeOf(t));
        assert t instanceof IdentifierTree && ((IdentifierTree) t).getName().contentEquals("this");
        tree = t;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitExplicitThis(this, p);
    }

    @Override
    public String toString() {
        return getName();
    }
}
