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
 * A visitor for {@link QualifiedTypeMirror}s, patterned after {@link
 * javax.lang.model.type.TypeVisitor}.
 */
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

    R visitTypeDeclaration(QualifiedTypeDeclaration<Q> type, P p);
    R visitParameterDeclaration(QualifiedParameterDeclaration<Q> type, P p);
}
