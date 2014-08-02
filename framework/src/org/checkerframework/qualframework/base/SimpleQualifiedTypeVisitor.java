package org.checkerframework.qualframework.base;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedArrayType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedIntersectionType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedNoType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedNullType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedPrimitiveType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedUnionType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedWildcardType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedParameterDeclaration;

/**
 * A simple implementation of {@link QualifiedTypeVisitor}, patterned after
 * {@link javax.lang.model.util.SimpleTypeVisitor8}.
 */
public class SimpleQualifiedTypeVisitor<Q,R,P> implements QualifiedTypeVisitor<Q,R,P> {
    protected final R DEFAULT_VALUE;

    public SimpleQualifiedTypeVisitor() {
        this(null);
    }

    public SimpleQualifiedTypeVisitor(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    protected R defaultAction(QualifiedTypeMirror<Q> type, P p) {
        return DEFAULT_VALUE;
    }

    @Override
    public R visit(QualifiedTypeMirror<Q> type) {
        return visit(type, null);
    }

    @Override
    public R visit(QualifiedTypeMirror<Q> type, P p) {
        return (type == null) ? null : type.accept(this, p);
    }

    @Override
    public R visitDeclared(QualifiedDeclaredType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitIntersection(QualifiedIntersectionType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitUnion(QualifiedUnionType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitExecutable(QualifiedExecutableType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitArray(QualifiedArrayType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitTypeVariable(QualifiedTypeVariable<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitPrimitive(QualifiedPrimitiveType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitNoType(QualifiedNoType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitNull(QualifiedNullType<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitWildcard(QualifiedWildcardType<Q> type, P p) {
        return defaultAction(type, p);
    }


    @Override
    public R visitTypeDeclaration(QualifiedTypeDeclaration<Q> type, P p) {
        return defaultAction(type, p);
    }

    @Override
    public R visitParameterDeclaration(QualifiedParameterDeclaration<Q> type, P p) {
        return defaultAction(type, p);
    }
}
