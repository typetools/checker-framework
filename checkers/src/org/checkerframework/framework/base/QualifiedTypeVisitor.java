package org.checkerframework.framework.base;

import org.checkerframework.framework.base.QualifiedTypeMirror;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedArrayType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedIntersectionType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedNoType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedNullType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedPrimitiveType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedTypeVariable;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedUnionType;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedWildcardType;

public interface QualifiedTypeVisitor<Q,R,P> {
    R visit(QualifiedTypeMirror<Q> type);
    R visit(QualifiedTypeMirror<Q> type, P p);
    R visitDeclared(QualifiedDeclaredType<Q> type, P p);
    R visitIntersection(QualifiedIntersectionType<Q> type, P p);
    R visitUnion(QualifiedUnionType<Q> type, P p);
    R visitExecutable(QualifiedExecutableType<Q> type, P p);
    R visitArray(QualifiedArrayType<Q> type, P p);
    R visitTypeVariable(QualifiedTypeVariable<Q> type, P p);
    R visitPrimitive(QualifiedPrimitiveType<Q> type, P p);
    R visitNoType(QualifiedNoType<Q> type, P p);
    R visitNull(QualifiedNullType<Q> type, P p);
    R visitWildcard(QualifiedWildcardType<Q> type, P p);
}
