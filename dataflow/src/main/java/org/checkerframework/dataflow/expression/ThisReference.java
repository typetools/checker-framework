package org.checkerframework.dataflow.expression;

import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.TypesUtils;

public class ThisReference extends JavaExpression {
    public ThisReference(TypeMirror type) {
        super(type);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ThisReference;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "this";
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz;
    }

    @Override
    public boolean syntacticEquals(JavaExpression je) {
        return je instanceof ThisReference;
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return this.syntacticEquals(other);
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        return true;
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return TypesUtils.isImmutableTypeInJdk(type);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return false; // 'this' is not modifiable
    }

    @Override
    public ThisReference atMethodSignature(List<JavaExpression> parameters) {
        return this;
    }
}
