package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ClassTree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node representing a class declaration that occurs within a method, for example, an anonymous
 * class declaration. In contrast to a top-level class declaration, such a declaration has an
 * initialization store that contains captured variables.
 */
public class ClassDeclarationNode extends Node {

    protected final ClassTree tree;

    public ClassDeclarationNode(ClassTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    @Override
    public ClassTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitClassDeclaration(this, p);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassDeclarationNode that = (ClassDeclarationNode) o;

        if (tree != null ? !tree.equals(that.tree) : that.tree != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return tree != null ? tree.hashCode() : 0;
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
