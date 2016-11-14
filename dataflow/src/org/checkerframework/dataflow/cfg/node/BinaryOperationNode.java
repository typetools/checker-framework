package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;
import java.util.Collection;
import java.util.LinkedList;
import org.checkerframework.javacutil.InternalUtils;

/**
 * A node for a binary expression.
 *
 * For example:
 * <pre>
 *   <em>lefOperandNode</em> <em>operator</em> <em>rightOperandNode</em>
 * </pre>
 *
 * @author charleszhuochen
 *
 */
public abstract class BinaryOperationNode extends Node {

    final protected BinaryTree tree;
    final protected Node left;
    final protected Node right;

    public BinaryOperationNode(BinaryTree tree, Node left, Node right) {
        super(InternalUtils.typeOf(tree));
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
        LinkedList<Node> list = new LinkedList<Node>();
        list.add(getLeftOperand());
        list.add(getRightOperand());
        return list;
    }
}
