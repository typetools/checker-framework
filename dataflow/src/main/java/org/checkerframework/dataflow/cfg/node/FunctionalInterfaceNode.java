package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A node for member references and lambdas.
 *
 * <p>The {@link Node#type} of a FunctionalInterfaceNode is determined by the assignment context the
 * member reference or lambda is used in.
 *
 * <pre>
 *   <em>FunctionalInterface func = param1, param2, ... &rarr; statement</em>
 * </pre>
 *
 * <pre>
 *   <em>FunctionalInterface func = param1, param2, ... &rarr; { ... }</em>
 * </pre>
 *
 * <pre>
 *   <em>FunctionalInterface func = member reference</em>
 * </pre>
 */
public class FunctionalInterfaceNode extends Node {

    protected final Tree tree;

    public FunctionalInterfaceNode(MemberReferenceTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    public FunctionalInterfaceNode(LambdaExpressionTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitMemberReference(this, p);
    }

    @Override
    public String toString() {
        if (tree instanceof LambdaExpressionTree) {
            return "FunctionalInterfaceNode:" + ((LambdaExpressionTree) tree).getBodyKind();
        } else if (tree instanceof MemberReferenceTree) {
            return "FunctionalInterfaceNode:" + ((MemberReferenceTree) tree).getName();
        } else {
            // This should never happen.
            throw new BugInCF("Invalid tree in FunctionalInterfaceNode");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FunctionalInterfaceNode that = (FunctionalInterfaceNode) o;

        return tree != null ? tree.equals(that.tree) : that.tree == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree);
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
