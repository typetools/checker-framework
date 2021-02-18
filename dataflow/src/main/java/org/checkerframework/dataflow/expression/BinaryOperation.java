package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.javacutil.AnnotationProvider;

/** JavaExpression for binary operations. */
public class BinaryOperation extends JavaExpression {

    /** The binary operation kind. */
    protected final Tree.Kind operationKind;
    /** The left operand. */
    protected final JavaExpression left;
    /** The right operand. */
    protected final JavaExpression right;

    /**
     * Create a binary operation.
     *
     * @param type the result type
     * @param operationKind the operator
     * @param left the left operand
     * @param right the right operand
     */
    public BinaryOperation(
            TypeMirror type, Tree.Kind operationKind, JavaExpression left, JavaExpression right) {
        super(type);
        this.operationKind = operationKind;
        this.left = left;
        this.right = right;
    }

    /**
     * Create a binary operation.
     *
     * @param node the binary operation node
     * @param left the left operand
     * @param right the right operand
     */
    public BinaryOperation(BinaryOperationNode node, JavaExpression left, JavaExpression right) {
        this(node.getType(), node.getTree().getKind(), left, right);
    }

    /**
     * Returns the operator of this binary operation.
     *
     * @return the binary operation kind
     */
    public Tree.Kind getOperationKind() {
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
    public boolean isDeterministic(AnnotationProvider provider) {
        return left.isDeterministic(provider) && right.isDeterministic(provider);
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
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof BinaryOperation)) {
            return false;
        }
        BinaryOperation other = (BinaryOperation) je;
        return operationKind == other.getOperationKind()
                && left.syntacticEquals(other.left)
                && right.syntacticEquals(other.right);
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return this.syntacticEquals(other)
                || left.containsSyntacticEqualJavaExpression(other)
                || right.containsSyntacticEqualJavaExpression(other);
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
        return left.toString()
                + " "
                + operationKindToString(operationKind)
                + " "
                + right.toString();
    }

    /**
     * Return the Java source code representation of the given operation.
     *
     * @param operationKind an unary operation kind
     * @return the Java source code representation of the given operation
     */
    private String operationKindToString(Tree.Kind operationKind) {
        switch (operationKind) {
            case CONDITIONAL_AND:
                return "&&";
            case AND:
                return "&";
            case OR:
                return "|";
            case DIVIDE:
                return "/";
            case EQUAL_TO:
                return "==";
            case GREATER_THAN:
                return ">";
            case GREATER_THAN_EQUAL:
                return ">=";
            case LEFT_SHIFT:
                return "<<";
            case LESS_THAN:
                return "<";
            case LESS_THAN_EQUAL:
                return "<=";
            case MINUS:
                return "-";
            case MULTIPLY:
                return "*";
            case NOT_EQUAL_TO:
                return "!=";
            case CONDITIONAL_OR:
                return "||";
            case PLUS:
                return "+";
            case REMAINDER:
                return "%";
            case RIGHT_SHIFT:
                return ">>";
            case UNSIGNED_RIGHT_SHIFT:
                return ">>>";
            case XOR:
                return "^";
            default:
                throw new Error("unhandled " + operationKind);
        }
    }
}
