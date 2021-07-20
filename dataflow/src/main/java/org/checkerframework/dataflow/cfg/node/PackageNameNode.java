package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.lang.model.element.Element;

/**
 * A node representing a package name used in an expression such as a constructor invocation.
 *
 * <p><em>package</em>.class.object(...)
 *
 * <p>parent.<em>package</em>.class.object(...)
 */
public class PackageNameNode extends Node {

    protected final Tree tree;
    /** The package named by this node. */
    protected final Element element;

    /** The parent name, if any. */
    protected final @Nullable PackageNameNode parent;

    public PackageNameNode(IdentifierTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        assert TreeUtils.isUseOfElement(tree) : "@AssumeAssertion(nullness): tree kind";
        this.element = TreeUtils.elementFromUse(tree);
        this.parent = null;
    }

    public PackageNameNode(MemberSelectTree tree, PackageNameNode parent) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        assert TreeUtils.isUseOfElement(tree) : "@AssumeAssertion(nullness): tree kind";
        this.element = TreeUtils.elementFromUse(tree);
        this.parent = parent;
    }

    public Element getElement() {
        return element;
    }

    /** The package name node for the parent package, {@code null} otherwise. */
    public @Nullable PackageNameNode getParent() {
        return parent;
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
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof PackageNameNode)) {
            return false;
        }
        PackageNameNode other = (PackageNameNode) obj;
        return Objects.equals(getParent(), other.getParent())
                && getElement().equals(other.getElement());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getElement(), getParent());
    }

    @Override
    public Collection<Node> getOperands() {
        if (parent == null) {
            return Collections.emptyList();
        }
        return Collections.singleton((Node) parent);
    }
}
