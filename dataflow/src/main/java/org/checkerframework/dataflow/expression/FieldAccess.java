package org.checkerframework.dataflow.expression;

import com.sun.tools.javac.code.Symbol;
import java.util.Objects;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

public class FieldAccess extends JavaExpression {
    protected final JavaExpression receiver;
    protected final VariableElement field;

    public JavaExpression getReceiver() {
        return receiver;
    }

    public VariableElement getField() {
        return field;
    }

    public FieldAccess(JavaExpression receiver, FieldAccessNode node) {
        super(node.getType());
        this.receiver = receiver;
        this.field = node.getElement();
    }

    public FieldAccess(JavaExpression receiver, TypeMirror type, VariableElement fieldElement) {
        super(type);
        this.receiver = receiver;
        this.field = fieldElement;
    }

    public boolean isFinal() {
        return ElementUtils.isFinal(field);
    }

    public boolean isStatic() {
        return ElementUtils.isStatic(field);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof FieldAccess)) {
            return false;
        }
        FieldAccess fa = (FieldAccess) obj;
        return fa.getField().equals(getField()) && fa.getReceiver().equals(getReceiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getField(), getReceiver());
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return super.containsModifiableAliasOf(store, other)
                || receiver.containsModifiableAliasOf(store, other);
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other) || receiver.containsSyntacticEqualJavaExpression(other);
    }

    @Override
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof FieldAccess)) {
            return false;
        }
        FieldAccess other = (FieldAccess) je;
        return this.receiver.syntacticEquals(other.receiver) && this.field.equals(other.field);
    }

    @Override
    public String toString() {
        if (receiver instanceof ClassName) {
            return receiver.getType() + "." + field;
        } else {
            return receiver + "." + field;
        }
    }

    @Override
    public String toStringDebug() {
        return String.format(
                "FieldAccess(type=%s, receiver=%s, field=%s [%s] [%s] owner=%s)",
                type,
                receiver.toStringDebug(),
                field,
                field.getClass().getSimpleName(),
                System.identityHashCode(field),
                ((Symbol) field).owner);
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz || receiver.containsOfClass(clazz);
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return isFinal() && getReceiver().isUnassignableByOtherCode();
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return isUnassignableByOtherCode() && TypesUtils.isImmutableTypeInJdk(getReceiver().type);
    }
}
