package org.checkerframework.dataflow.expression;

import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.ValueLiteralNode;
import org.checkerframework.javacutil.TypesUtils;

/** FlowExpression.JavaExpression for literals. */
public class ValueLiteral extends JavaExpression {

    /** The value of the literal. */
    protected final @Nullable Object value;

    /**
     * Creates a ValueLiteral from the node with the given type.
     *
     * @param type type of the literal
     * @param node the literal represents by this {@link
     *     org.checkerframework.dataflow.expression.ValueLiteral}
     */
    public ValueLiteral(TypeMirror type, ValueLiteralNode node) {
        super(type);
        value = node.getValue();
    }

    /**
     * Creates a ValueLiteral where the value is {@code value} that has the given type.
     *
     * @param type type of the literal
     * @param value the literal value
     */
    public ValueLiteral(TypeMirror type, Object value) {
        super(type);
        this.value = value;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz;
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return true;
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ValueLiteral)) {
            return false;
        }
        ValueLiteral other = (ValueLiteral) obj;
        // TODO:  Can this string comparison be cleaned up?
        // Cannot use Types.isSameType(type, other.type) because we don't have a Types object.
        return type.toString().equals(other.type.toString()) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        if (TypesUtils.isString(type)) {
            return "\"" + value + "\"";
        } else if (type.getKind() == TypeKind.LONG) {
            assert value != null : "@AssumeAssertion(nullness): invariant";
            return value.toString() + "L";
        } else if (type.getKind() == TypeKind.CHAR) {
            return "\'" + value + "\'";
        }
        return value == null ? "null" : value.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type.toString());
    }

    @Override
    public boolean syntacticEquals(JavaExpression other) {
        return this.equals(other);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return false; // not modifiable
    }

    /**
     * Returns the value of this literal.
     *
     * @return the value of this literal
     */
    public @Nullable Object getValue() {
        return value;
    }
}
