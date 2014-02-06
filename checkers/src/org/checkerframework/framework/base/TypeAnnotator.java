package org.checkerframework.framework.base;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.AbstractTypeVisitor8;

import org.checkerframework.framework.util.ExtendedArrayType;
import org.checkerframework.framework.util.ExtendedDeclaredType;
import org.checkerframework.framework.util.ExtendedErrorType;
import org.checkerframework.framework.util.ExtendedExecutableType;
import org.checkerframework.framework.util.ExtendedIntersectionType;
import org.checkerframework.framework.util.ExtendedNoType;
import org.checkerframework.framework.util.ExtendedNullType;
import org.checkerframework.framework.util.ExtendedPrimitiveType;
import org.checkerframework.framework.util.ExtendedTypeVariable;
import org.checkerframework.framework.util.ExtendedUnionType;
import org.checkerframework.framework.util.ExtendedWildcardType;
import org.checkerframework.framework.util.ExtendedTypeMirror;
import org.checkerframework.framework.util.ExtendedTypeVisitor;

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

public class TypeAnnotator<Q> implements ExtendedTypeVisitor<QualifiedTypeMirror<Q>, Element> {
    private AnnotationConverter<Q> annotationConverter;
    private Q topQual;
    private Q bottomQual;

    public TypeAnnotator(AnnotationConverter<Q> annotationConverter,
            Q topQual, Q bottomQual) {
        this.annotationConverter = annotationConverter;
        this.topQual = topQual;
        this.bottomQual = bottomQual;
    }

    public QualifiedTypeMirror<Q> visit(ExtendedTypeMirror type, Element elt) {
        if (type == null) {
            return null;
        }
        return type.accept(this, elt);
    }

    private List<QualifiedTypeMirror<Q>> mapVisit(
            List<? extends ExtendedTypeMirror> types, Element elt) {
        List<QualifiedTypeMirror<Q>> result = new ArrayList<>();
        for (ExtendedTypeMirror type : types) {
            result.add(this.visit(type, elt));
        }
        return result;
    }

    protected Q getQualifier(ExtendedTypeMirror type, Element elt) {
        Q qual = annotationConverter.fromAnnotations(type.getAnnotationMirrors());
        if (qual == null) {
            qual = topQual;
        }
        return qual;
    }

    @Override
    public QualifiedTypeMirror<Q> visitArray(ExtendedArrayType type, Element elt) {
        return new QualifiedArrayType<Q>(
                type,
                getQualifier(type, elt),
                this.visit(type.getComponentType(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitDeclared(ExtendedDeclaredType type, Element elt) {
        QualifiedTypeMirror<Q> result = new QualifiedDeclaredType<Q>(
                type,
                getQualifier(type, elt),
                this.mapVisit(type.getTypeArguments(), elt));

        return result;
    }

    @Override
    public QualifiedTypeMirror<Q> visitError(ExtendedErrorType type, Element elt) {
        throw new UnsupportedOperationException("saw unexpected ExtendedErrorType");
    }

    @Override
    public QualifiedTypeMirror<Q> visitExecutable(ExtendedExecutableType type, Element elt) {
        // QualifiedExecutableType requires a list of QualifiedTypeVariables
        // rather than a list of generic QualifiedTypeMirrors.
        List<QualifiedTypeVariable<Q>> qualifiedTypeVariables = new ArrayList<>();
        for (ExtendedTypeVariable typeVar : type.getTypeVariables()) {
            @SuppressWarnings("unchecked")
            QualifiedTypeVariable<Q> qualifiedTypeVar =
                (QualifiedTypeVariable<Q>)this.visit(typeVar, elt);
            qualifiedTypeVariables.add(qualifiedTypeVar);
        }

        return new QualifiedExecutableType<Q>(
                type,
                getQualifier(type, elt),
                this.mapVisit(type.getParameterTypes(), elt),
                this.visit(type.getReceiverType(), elt),
                this.visit(type.getReturnType(), elt),
                this.mapVisit(type.getThrownTypes(), elt),
                qualifiedTypeVariables);
    }

    @Override
    public QualifiedTypeMirror<Q> visitIntersection(ExtendedIntersectionType type, Element elt) {
        return new QualifiedIntersectionType<Q>(
                type,
                getQualifier(type, elt),
                this.mapVisit(type.getBounds(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitNoType(ExtendedNoType type, Element elt) {
        return new QualifiedNoType<Q>(
                type,
                getQualifier(type, elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitNull(ExtendedNullType type, Element elt) {
        return new QualifiedNullType<Q>(
                type,
                getQualifier(type, elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitPrimitive(ExtendedPrimitiveType type, Element elt) {
        return new QualifiedPrimitiveType<Q>(
                type,
                getQualifier(type, elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeVariable(ExtendedTypeVariable type, Element elt) {
        return new QualifiedTypeVariable<Q>(
                type,
                getQualifier(type, elt),
                this.visit(type.getUpperBound(), elt),
                this.visit(type.getLowerBound(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnion(ExtendedUnionType type, Element elt) {
        return new QualifiedUnionType<Q>(
                type,
                getQualifier(type, elt),
                this.mapVisit(type.getAlternatives(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitWildcard(ExtendedWildcardType type, Element elt) {
        return new QualifiedWildcardType<Q>(
                type,
                getQualifier(type, elt),
                this.visit(type.getExtendsBound(), elt),
                this.visit(type.getSuperBound(), elt));
    }
}
