package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.UnaryOperationNode;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;

import java.util.Objects;

import javax.lang.model.type.TypeMirror;

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
     * Create a unary operation.
     *
     * @param node the unary operation node
     * @param operand the operand
     */
    public UnaryOperation(UnaryOperationNode node, JavaExpression operand) {
        this(node.getType(), node.getTree().getKind(), operand);
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
    public boolean isDeterministic(AnnotationProvider provider) {
        return operand.isDeterministic(provider);
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
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof UnaryOperation)) {
            return false;
        }
        UnaryOperation other = (UnaryOperation) je;
        return operationKind == other.getOperationKind() && operand.syntacticEquals(other.operand);
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return this.syntacticEquals(other) || operand.containsSyntacticEqualJavaExpression(other);
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

    @Override
    public String toString() {
        String operandString = operand.toString();
        switch (operationKind) {
            case BITWISE_COMPLEMENT:
                return "~" + operandString;
            case LOGICAL_COMPLEMENT:
                return "!" + operandString;
            case POSTFIX_DECREMENT:
                return operandString + "--";
            case POSTFIX_INCREMENT:
                return operandString + "++";
            case PREFIX_DECREMENT:
                return "--" + operandString;
            case PREFIX_INCREMENT:
                return "++" + operandString;
            case UNARY_MINUS:
                return "-" + operandString;
            case UNARY_PLUS:
                return "+" + operandString;
            default:
                throw new BugInCF("Unrecognized unary operation kind " + operationKind);
        }
    }

    @Override
    public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
        return visitor.visitUnaryOperation(this, p);
    }
}
