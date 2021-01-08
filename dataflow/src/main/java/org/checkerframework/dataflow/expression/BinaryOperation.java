package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;

/** JavaExpression for binary operations. */
public class BinaryOperation extends JavaExpression {

    /** The binary operation kind. */
    protected final Kind operationKind;
    /** The binary operation kind for pretty printing. */
    protected final JCTree.Tag tag;
    /** The left operand. */
    protected final JavaExpression left;
    /** The right operand. */
    protected final JavaExpression right;

    /**
     * Create a binary operation.
     *
     * @param node the binary operation node
     * @param left the left operand
     * @param right the right operand
     */
    public BinaryOperation(BinaryOperationNode node, JavaExpression left, JavaExpression right) {
        super(node.getType());
        this.operationKind = node.getTree().getKind();
        this.tag = ((JCTree) node.getTree()).getTag();
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the operator of this binary operation.
     *
     * @return the binary operation kind
     */
    public Kind getOperationKind() {
        return operationKind;
    }

    /**
     * Returns the left operand of this binary operation.
     *
     * @return the left operand
     */
    public JavaExpression getLeft() {
        return left;
    }

    /**
     * Returns the right operand of this binary operation.
     *
     * @return the right operand
     */
    public JavaExpression getRight() {
        return right;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        if (getClass() == clazz) {
            return true;
        }
        return left.containsOfClass(clazz) || right.containsOfClass(clazz);
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return left.isUnassignableByOtherCode() && right.isUnassignableByOtherCode();
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return left.isUnmodifiableByOtherCode() && right.isUnmodifiableByOtherCode();
    }

    @Override
    public boolean syntacticEquals(JavaExpression other) {
        if (!(other instanceof BinaryOperation)) {
            return false;
        }
        BinaryOperation biOp = (BinaryOperation) other;
        if (!(operationKind == biOp.getOperationKind())) {
            return false;
        }
        return left.equals(biOp.left) && right.equals(biOp.right);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return left.containsModifiableAliasOf(store, other)
                || right.containsModifiableAliasOf(store, other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationKind, left, right);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof BinaryOperation)) {
            return false;
        }
        BinaryOperation biOp = (BinaryOperation) other;
        if (!(operationKind == biOp.getOperationKind())) {
            return false;
        }
        if (isCommutative()) {
            return (left.equals(biOp.left) && right.equals(biOp.right))
                    || (left.equals(biOp.right) && right.equals(biOp.left));
        }
        return left.equals(biOp.left) && right.equals(biOp.right);
    }

    /**
     * Returns true if the binary operation is commutative, e.g., x + y == y + x.
     *
     * @return true if the binary operation is commutative
     */
    private boolean isCommutative() {
        switch (operationKind) {
            case PLUS:
            case MULTIPLY:
            case AND:
            case OR:
            case XOR:
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case CONDITIONAL_AND:
            case CONDITIONAL_OR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        final Pretty pretty = new Pretty(null, true);
        StringBuilder result = new StringBuilder();
        result.append(left.toString());
        result.append(pretty.operatorName(tag));
        result.append(right.toString());
        return result.toString();
    }
}
