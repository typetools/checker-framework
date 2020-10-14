package org.checkerframework.dataflow.expression;

import java.util.Objects;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

public class FieldAccess extends Receiver {
    protected final Receiver receiver;
    protected final VariableElement field;

    public Receiver getReceiver() {
        return receiver;
    }

    public VariableElement getField() {
        return field;
    }

    public FieldAccess(Receiver receiver, FieldAccessNode node) {
        super(node.getType());
        this.receiver = receiver;
        this.field = node.getElement();
    }

    public FieldAccess(Receiver receiver, TypeMirror type, VariableElement fieldElement) {
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
    public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
        return super.containsModifiableAliasOf(store, other)
                || receiver.containsModifiableAliasOf(store, other);
    }

    @Override
    public boolean containsSyntacticEqualReceiver(Receiver other) {
        return syntacticEquals(other) || receiver.containsSyntacticEqualReceiver(other);
    }

    @Override
    public boolean syntacticEquals(Receiver other) {
        if (!(other instanceof FieldAccess)) {
            return false;
        }
        FieldAccess fa = (FieldAccess) other;
        return super.syntacticEquals(other)
                || (fa.getField().equals(getField())
                        && fa.getReceiver().syntacticEquals(getReceiver()));
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
    public boolean containsOfClass(Class<? extends Receiver> clazz) {
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
