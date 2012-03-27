package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A node representing a package name used in an expression
 * such as a constructor invocation
 * 
 * <em>package</em>.class.object(...)
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class PackageNameNode extends Node {

    protected Tree tree;
    // The package named by this node
    protected Element element;

    public PackageNameNode(Tree tree) {
        assert tree.getKind() == Tree.Kind.IDENTIFIER;
        this.tree = tree;
        this.type = InternalUtils.typeOf(tree);
        this.element = TreeUtils.elementFromUse((IdentifierTree) tree);
    }

    public Element getElement() {
        return element;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitPackageName(this, p);
    }

    @Override
    public String toString() {
        return getElement().getSimpleName().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PackageNameNode)) {
            return false;
        }
        PackageNameNode other = (PackageNameNode) obj;
        return getElement().equals(other.getElement());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getElement());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
