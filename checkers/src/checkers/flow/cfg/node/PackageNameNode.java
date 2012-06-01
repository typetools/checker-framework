package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

/**
 * A node representing a package name used in an expression
 * such as a constructor invocation
 * 
 * <p>
 * <em>package</em>.class.object(...)
 * <p>
 * parent.<em>package</em>.class.object(...)
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class PackageNameNode extends Node {

    protected final Tree tree;
    // The package named by this node
    protected final Element element;
    
    /** The parent name, if any. */
    protected final /*@Nullable*/PackageNameNode parent;

    public PackageNameNode(IdentifierTree tree) {
        this.tree = tree;
        this.type = InternalUtils.typeOf(tree);
        this.element = TreeUtils.elementFromUse(tree);
        this.parent = null;
    }
    
    public PackageNameNode(MemberSelectTree tree, PackageNameNode parent) {
        this.tree = tree;
        this.type = InternalUtils.typeOf(tree);
        this.element = TreeUtils.elementFromUse(tree);
        this.parent = parent;
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
