package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for a postfix or an unary expression.
 *
 * <p>For example:
 *
 * <pre>
 *   <em>operator</em> <em>expressionNode</em>
 *
 *   <em>expressionNode</em> <em>operator</em>
 * </pre>
 */
public abstract class UnaryOperationNode extends Node {

    // The tree might not be a unary tree, if this is a ConditionalNot created from an occurrence of
    // "... != true" in the source code.
    protected final Tree tree;
    protected final Node operand;

    protected UnaryOperationNode(Tree tree, Node operand) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.operand = operand;
    }

    public Node getOperand() {
        return this.operand;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(getOperand());
    }
}
