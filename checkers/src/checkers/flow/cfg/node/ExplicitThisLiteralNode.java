package checkers.flow.cfg.node;

import checkers.util.InternalUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;

/**
 * A node for a reference to 'this'.
 * 
 * <pre>
 *   <em>this</em>
 * </pre>
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class ExplicitThisLiteralNode extends ThisLiteralNode {

    protected Tree tree;

    public ExplicitThisLiteralNode(Tree t) {
        assert t instanceof IdentifierTree
                && ((IdentifierTree) t).getName().contentEquals("this");
        tree = t;
        type = InternalUtils.typeOf(tree);
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitExplicitThisLiteral(this, p);
    }

    @Override
    public String toString() {
        return getName();
    }
}
