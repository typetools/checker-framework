package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a binary expression.
 *
 * <p>For example:
 *
 * <pre>
 *   <em>lefOperandNode</em> <em>operator</em> <em>rightOperandNode</em>
 * </pre>
 *
 * @author charleszhuochen
 */
public abstract class BinaryOperationNode extends Node {

    protected final BinaryTree tree;
    protected final Node left;
    protected final Node right;

    public BinaryOperationNode(BinaryTree tree, Node left, Node right) {
        super(TreeUtils.typeOf(tree));
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
    public BinaryTree getTree() {
        return tree;
    }

    @Override
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<Node>(2);
        list.add(getLeftOperand());
        list.add(getRightOperand());
        return list;
    }
}
