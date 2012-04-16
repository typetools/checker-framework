package checkers.flow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import checkers.flow.util.HashCodeUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;

/**
 * A node representing a class name used in an expression
 * such as a static method invocation.
 * 
 * <em>class</em> .forName(...)
 * 
 * @author Stefan Heule
 * @author Charlie Garrett
 * 
 */
public class ClassNameNode extends Node {

    protected Tree tree;
    // The class named by this node
    protected Element element;

    public ClassNameNode(Tree tree) {
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
        return visitor.visitClassName(this, p);
    }

    @Override
    public String toString() {
        return getElement().getSimpleName().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ClassNameNode)) {
            return false;
        }
        ClassNameNode other = (ClassNameNode) obj;
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
