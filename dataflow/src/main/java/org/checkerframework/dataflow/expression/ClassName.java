package org.checkerframework.dataflow.expression;

import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;

/**
 * A ClassName represents the occurrence of a class as part of a static field access or method
 * invocation.
 */
public class ClassName extends JavaExpression {
    private final String typeString;

    public ClassName(TypeMirror type) {
        super(type);
        typeString = type.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ClassName)) {
            return false;
        }
        ClassName other = (ClassName) obj;
        return typeString.equals(other.typeString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeString);
    }

    @Override
    public String toString() {
        return typeString + ".class";
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz;
    }

    @Override
    public boolean syntacticEquals(JavaExpression other) {
        return this.equals(other);
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
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return false; // not modifiable
    }
}
