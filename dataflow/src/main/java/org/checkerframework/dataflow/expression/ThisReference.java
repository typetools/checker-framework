package org.checkerframework.dataflow.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.type.TypeMirror;

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
    public boolean isDeterministic(AnnotationProvider provider) {
        return true;
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
    public boolean syntacticEquals(JavaExpression je) {
        return je instanceof ThisReference;
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return this.syntacticEquals(other);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return false; // 'this' is not modifiable
    }

    @Override
    public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
        return visitor.visitThisReference(this, p);
    }
}
