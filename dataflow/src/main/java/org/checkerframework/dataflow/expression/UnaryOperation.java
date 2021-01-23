package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;

/** JavaExpression for unary operations. */
public class UnaryOperation extends JavaExpression {

    /** The unary operation kind. */
    protected final Tree.Kind operationKind;
    /** The operand. */
    protected final JavaExpression operand;

    /**
     * Create a unary operation.
     *
     * @param type the type of the result
     * @param operationKind the operator
     * @param operand the operand
     */
    public UnaryOperation(TypeMirror type, Tree.Kind operationKind, JavaExpression operand) {
        super(operand.type);
        this.operationKind = operationKind;
        this.operand = operand;
    }

    /**
     * Returns the operator of this unary operation.
     *
     * @return the unary operation kind
     */
    public Tree.Kind getOperationKind() {
        return operationKind;
    }

    /**
     * Returns the operand of this unary operation.
     *
     * @return the operand
     */
    public JavaExpression getOperand() {
        return operand;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        if (getClass() == clazz) {
            return true;
        }
        return operand.containsOfClass(clazz);
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return operand.isUnassignableByOtherCode();
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return operand.isUnmodifiableByOtherCode();
    }

    @Override
    public boolean syntacticEquals(JavaExpression other) {
        if (!(other instanceof UnaryOperation)) {
            return false;
        }
        UnaryOperation unOp = (UnaryOperation) other;
        return operationKind == unOp.getOperationKind() && operand.equals(unOp.operand);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return operand.containsModifiableAliasOf(store, other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationKind, operand);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof UnaryOperation)) {
            return false;
        }
        UnaryOperation unOp = (UnaryOperation) other;
        return operationKind == unOp.getOperationKind() && operand.equals(unOp.operand);
    }

    /**
     * Return the Java source code representation of the given operation.
     *
     * @param operationKind an unary operation kind
     * @return the Java source code representation of the given operation
     */
    private String operationKindToString(Tree.Kind operationKind) {
        switch (operationKind) {
            case UNARY_MINUS:
                return "-";
            default:
                throw new Error("Not yet implemented " + operationKind);
        }
    }

    @Override
    public String toString() {
        return operationKindToString(operationKind) + operand.toString();
    }
}
