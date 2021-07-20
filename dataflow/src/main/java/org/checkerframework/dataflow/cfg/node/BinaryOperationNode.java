package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.BinaryTree;

import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * A node for a binary expression.
 *
 * <p>For example:
 *
 * <pre>
 *   <em>lefOperandNode</em> <em>operator</em> <em>rightOperandNode</em>
 * </pre>
 */
public abstract class BinaryOperationNode extends Node {

    protected final BinaryTree tree;
    protected final Node left;
    protected final Node right;

    protected BinaryOperationNode(BinaryTree tree, Node left, Node right) {
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
        return Arrays.asList(getLeftOperand(), getRightOperand());
    }
}
