package org.checkerframework.qualframework.util;

import java.util.*;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.*;
import org.checkerframework.qualframework.base.QualifiedTypeVisitor;


/** Visitor that replaces the qualifier of a {@link QualifiedTypeMirror}.
 */
public class SetQualifierVisitor<Q> implements QualifiedTypeVisitor<Q, QualifiedTypeMirror<Q>, Q> {

    @Override
    public QualifiedTypeMirror<Q> visit(QualifiedTypeMirror<Q> type) {
        return visit(type, null);
    }

    @Override
    public QualifiedTypeMirror<Q> visit(QualifiedTypeMirror<Q> type, Q newQual) {
        if (type == null) {
            return null;
        }
        return type.accept(this, newQual);
    }


    @Override
    public QualifiedTypeMirror<Q> visitArray(QualifiedArrayType<Q> type, Q newQual) {
        return new QualifiedArrayType<Q>(type.getUnderlyingType(),
                newQual,
                type.getComponentType()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitDeclared(QualifiedDeclaredType<Q> type, Q newQual) {
        return new QualifiedDeclaredType<Q>(type.getUnderlyingType(),
                newQual,
                type.getTypeArguments()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitExecutable(QualifiedExecutableType<Q> type, Q newQual) {
        return new QualifiedExecutableType<Q>(type.getUnderlyingType(),
                newQual,
                type.getParameterTypes(),
                type.getReceiverType(),
                type.getReturnType(),
                type.getThrownTypes(),
                type.getTypeVariables()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitIntersection(QualifiedIntersectionType<Q> type, Q newQual) {
        return new QualifiedIntersectionType<Q>(type.getUnderlyingType(),
                newQual,
                type.getBounds()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitNoType(QualifiedNoType<Q> type, Q newQual) {
        return new QualifiedNoType<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitNull(QualifiedNullType<Q> type, Q newQual) {
        return new QualifiedNullType<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitPrimitive(QualifiedPrimitiveType<Q> type, Q newQual) {
        return new QualifiedPrimitiveType<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeVariable(QualifiedTypeVariable<Q> type, Q newQual) {
        return new QualifiedTypeVariable<Q>(type.getUnderlyingType(),
                newQual
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnion(QualifiedUnionType<Q> type, Q newQual) {
        return new QualifiedUnionType<Q>(type.getUnderlyingType(),
                newQual,
                type.getAlternatives()
                );
    }

    @Override
    public QualifiedTypeMirror<Q> visitWildcard(QualifiedWildcardType<Q> type, Q newQual) {
        return new QualifiedWildcardType<Q>(type.getUnderlyingType(),
                newQual,
                type.getExtendsBound(),
                type.getSuperBound()
                );
    }
}
