package org.checkerframework.dataflow.expression;

import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;

/** An array access. */
public class ArrayAccess extends JavaExpression {

    protected final JavaExpression receiver;
    protected final JavaExpression index;

    public ArrayAccess(TypeMirror type, JavaExpression receiver, JavaExpression index) {
        super(type);
        this.receiver = receiver;
        this.index = index;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        if (getClass() == clazz) {
            return true;
        }
        if (receiver.containsOfClass(clazz)) {
            return true;
        }
        return index.containsOfClass(clazz);
    }

    public JavaExpression getReceiver() {
        return receiver;
    }

    public JavaExpression getIndex() {
        return index;
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return false;
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return false;
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other)
                || receiver.syntacticEquals(other)
                || index.syntacticEquals(other);
    }

    @Override
    public boolean syntacticEquals(JavaExpression other) {
        if (!(other instanceof ArrayAccess)) {
            return false;
        }
        ArrayAccess otherArrayAccess = (ArrayAccess) other;
        if (!receiver.syntacticEquals(otherArrayAccess.receiver)) {
            return false;
        }
        return index.syntacticEquals(otherArrayAccess.index);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        if (receiver.containsModifiableAliasOf(store, other)) {
            return true;
        }
        return index.containsModifiableAliasOf(store, other);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ArrayAccess)) {
            return false;
        }
        ArrayAccess other = (ArrayAccess) obj;
        return receiver.equals(other.receiver) && index.equals(other.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiver, index);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(receiver.toString());
        result.append("[");
        result.append(index.toString());
        result.append("]");
        return result.toString();
    }
}
