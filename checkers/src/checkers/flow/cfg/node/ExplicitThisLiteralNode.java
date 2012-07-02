package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import checkers.flow.util.HashCodeUtils;
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
public class ExplicitThisLiteralNode extends Node {

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

    public String getName() {
        return "this";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ExplicitThisLiteralNode)) {
            return false;
        }
        return true;
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
