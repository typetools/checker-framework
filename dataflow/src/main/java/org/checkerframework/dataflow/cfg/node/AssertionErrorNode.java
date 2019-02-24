package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * A node for the {@link AssertionError} when an assertion fails.
 *
 * <pre>
 *   assert <em>condition</em> : <em>detail</em> ;
 * </pre>
 */
public class AssertionErrorNode extends Node {

    protected final Tree tree;
    protected final Node condition;
    protected final Node detail;

    public AssertionErrorNode(Tree tree, Node condition, Node detail, TypeMirror type) {
        // TODO: Find out the correct "type" for statements.
        // Is it TypeKind.NONE?
        super(type);
        assert tree.getKind() == Kind.ASSERT;
        this.tree = tree;
        this.condition = condition;
        this.detail = detail;
    }

    public Node getCondition() {
        return condition;
    }

    public Node getDetail() {
        return detail;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitAssertionError(this, p);
    }

    @Override
    public String toString() {
        return "AssertionError(" + getDetail() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AssertionErrorNode)) {
            return false;
        }
        AssertionErrorNode other = (AssertionErrorNode) obj;
        return Objects.equals(getCondition(), other.getCondition())
                && Objects.equals(getDetail(), other.getDetail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCondition(), getDetail());
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(2);
        list.add(getCondition());
        list.add(getDetail());
        return list;
    }
}
