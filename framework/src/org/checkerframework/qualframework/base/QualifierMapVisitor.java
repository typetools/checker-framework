package org.checkerframework.qualframework.base;

import java.util.*;

import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.*;
import org.checkerframework.qualframework.base.QualifiedTypeVisitor;


/** Visitor that maps a function over every qualifier in a {@link
 * QualifiedTypeMirror}. */
public abstract class QualifierMapVisitor<Q,R,P> implements QualifiedTypeVisitor<Q, QualifiedTypeMirror<R>, P> {

    public abstract R process(Q qual, P p);

    @Override
    public QualifiedTypeMirror<R> visit(QualifiedTypeMirror<Q> type) {
        return visit(type, null);
    }

    @Override
    public QualifiedTypeMirror<R> visit(QualifiedTypeMirror<Q> type, P p) {
        if (type == null) {
            return null;
        }
        return type.accept(this, p);
    }


    private List<QualifiedTypeMirror<R>> visitTypes(List<? extends QualifiedTypeMirror<Q>> types, P p) {
        List<QualifiedTypeMirror<R>> results = new ArrayList<>();
        for (QualifiedTypeMirror<Q> type : types) {
            results.add(visit(type, p));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<QualifiedParameterDeclaration<R>> visitTypeParameters(List<? extends QualifiedParameterDeclaration<Q>> types, P p) {
        return (List<QualifiedParameterDeclaration<R>>)(List<?>)visitTypes(types, p);
    }


    @Override
    public QualifiedTypeMirror<R> visitArray(QualifiedArrayType<Q> type, P p) {
        return new QualifiedArrayType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p),
                visit(type.getComponentType(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitDeclared(QualifiedDeclaredType<Q> type, P p) {
        return new QualifiedDeclaredType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p),
                visitTypes(type.getTypeArguments(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitExecutable(QualifiedExecutableType<Q> type, P p) {
        return new QualifiedExecutableType<R>(type.getUnderlyingType(),
                visitTypes(type.getParameterTypes(), p),
                visit(type.getReceiverType(), p),
                visit(type.getReturnType(), p),
                visitTypes(type.getThrownTypes(), p),
                visitTypeParameters(type.getTypeParameters(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitIntersection(QualifiedIntersectionType<Q> type, P p) {
        return new QualifiedIntersectionType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p),
                visitTypes(type.getBounds(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitNoType(QualifiedNoType<Q> type, P p) {
        return new QualifiedNoType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitNull(QualifiedNullType<Q> type, P p) {
        return new QualifiedNullType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitPrimitive(QualifiedPrimitiveType<Q> type, P p) {
        return new QualifiedPrimitiveType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitTypeVariable(QualifiedTypeVariable<Q> type, P p) {
        return new QualifiedTypeVariable<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitUnion(QualifiedUnionType<Q> type, P p) {
        return new QualifiedUnionType<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p),
                visitTypes(type.getAlternatives(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitWildcard(QualifiedWildcardType<Q> type, P p) {
        return new QualifiedWildcardType<R>(type.getUnderlyingType(),
                visit(type.getExtendsBound(), p),
                visit(type.getSuperBound(), p)
                );
    }


    @Override
    public QualifiedTypeMirror<R> visitTypeDeclaration(QualifiedTypeDeclaration<Q> type, P p) {
        return new QualifiedTypeDeclaration<R>(type.getUnderlyingType(),
                process(type.getQualifier(), p),
                visitTypeParameters(type.getTypeParameters(), p)
                );
    }

    @Override
    public QualifiedTypeMirror<R> visitParameterDeclaration(QualifiedParameterDeclaration<Q> type, P p) {
        return new QualifiedParameterDeclaration<R>(type.getUnderlyingType());
    }
}
