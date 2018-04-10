package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for the string concatenation compound assignment:
 *
 * <pre>
 *   <em>variable</em> += <em>expression</em>
 * </pre>
 */
public class StringConcatenateAssignmentNode extends Node {
    protected final Tree tree;
    protected final Node left;
    protected final Node right;

    public StringConcatenateAssignmentNode(Tree tree, Node left, Node right) {
        super(TreeUtils.typeOf(tree));
        assert tree.getKind() == Kind.PLUS_ASSIGNMENT;
        this.tree = tree;
        this.left = left;
        this.right = right;
    }

    public Node getLeftOperand() {
        return left;
    }

    public Node getRightOperand() {
        return right;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitStringConcatenateAssignment(this, p);
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(2);
        list.add(getLeftOperand());
        list.add(getRightOperand());
        return list;
    }
}
