package org.checkerframework.dataflow.expression;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.ValueLiteralNode;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

/** JavaExpression for literals. */
public class ValueLiteral extends JavaExpression {

    /** The value of the literal. */
    protected final @Nullable Object value;

    /** The negative of Long.MIN_VALUE, which does not fit in a long. */
    private final BigInteger NEGATIVE_LONG_MIN_VALUE = new BigInteger("9223372036854775808");

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

    /**
     * Returns the negation of this literal. Throws an exception if negation is not possible.
     *
     * @return the negation of this literal
     */
    public ValueLiteral negate() {
        if (TypesUtils.isIntegralPrimitive(type)) {
            if (value == null) {
                throw new Error("null value of integral type " + type);
            }
            return new ValueLiteral(type, negateBoxedPrimitive(value));
        }
        throw new Error(String.format("cannot negate: %s type=%s", this, type));
    }

    /**
     * Negate a boxed primitive.
     *
     * @param o a boxed primitive
     * @return a boxed primitive that is the negation of the argument
     */
    private Object negateBoxedPrimitive(Object o) {
        if (value instanceof Byte) {
            return (byte) -(Byte) value;
        }
        if (value instanceof Short) {
            return (short) -(Short) value;
        }
        if (value instanceof Integer) {
            return -(Integer) value;
        }
        if (value instanceof Long) {
            return -(Long) value;
        }
        if (value instanceof Float) {
            return -(Float) value;
        }
        if (value instanceof Double) {
            return -(Double) value;
        }
        if (value instanceof BigInteger) {
            assert value.equals(NEGATIVE_LONG_MIN_VALUE);
            return Long.MIN_VALUE;
        }
        throw new Error("Cannot be negated: " + o + " " + o.getClass());
    }

    /**
     * Returns the value of this literal.
     *
     * @return the value of this literal
     */
    public @Nullable Object getValue() {
        return value;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz;
    }

    @Override
    public boolean isDeterministic(AnnotationProvider provider) {
        return true;
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
    public boolean syntacticEquals(JavaExpression je) {
        return this.equals(je);
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return this.syntacticEquals(other);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return false; // not modifiable
    }

    /// java.lang.Object methods

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
    public ValueLiteral atMethodSignature(List<JavaExpression> parameters) {
        return this;
    }
}
