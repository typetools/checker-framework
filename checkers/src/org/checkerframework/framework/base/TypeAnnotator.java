package org.checkerframework.framework.base;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractTypeVisitor8;

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

public class TypeAnnotator<Q> extends TypeVisitor2<QualifiedTypeMirror<Q>, Element> {
    private AnnotationConverter<Q> annotationConverter;

    public TypeAnnotator(AnnotationConverter<Q> annotationConverter) {
        this.annotationConverter = annotationConverter;
    }

    private List<QualifiedTypeMirror<Q>> mapVisit(
            List<? extends TypeMirror> types, Element elt) {
        List<QualifiedTypeMirror<Q>> result = new ArrayList<>();
        for (TypeMirror type : types) {
            result.add(this.visit(type, elt));
        }
        return result;
    }

    @Override
    public QualifiedTypeMirror<Q> visitArray(ArrayType type, Element elt) {
        return new QualifiedArrayType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.visit(type.getComponentType(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitDeclared(DeclaredType type, Element elt) {
        QualifiedTypeMirror<Q> result = new QualifiedDeclaredType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.mapVisit(type.getTypeArguments(), elt));

        return result;
    }

    @Override
    public QualifiedTypeMirror<Q> visitError(ErrorType type, Element elt) {
        throw new UnsupportedOperationException("saw unexpected ErrorType");
    }

    @Override
    public QualifiedTypeMirror<Q> visitExecutable(ExecutableType type, Element elt) {
        // QualifiedExecutableType requires a list of QualifiedTypeVariables
        // rather than a list of generic QualifiedTypeMirrors.
        List<QualifiedTypeVariable<Q>> qualifiedTypeVariables = new ArrayList<>();
        for (TypeVariable typeVar : type.getTypeVariables()) {
            @SuppressWarnings("unchecked")
            QualifiedTypeVariable<Q> qualifiedTypeVar =
                (QualifiedTypeVariable<Q>)this.visit(typeVar);
            qualifiedTypeVariables.add(qualifiedTypeVar);
        }

        QualifiedTypeMirror<Q> qualifiedReceiverType = null;
        if (type.getReceiverType() != null) {
            qualifiedReceiverType = this.visit(type.getReceiverType(), elt);
        }

        return new QualifiedExecutableType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.mapVisit(type.getParameterTypes(), elt),
                qualifiedReceiverType,
                this.visit(type.getReturnType(), elt),
                this.mapVisit(type.getThrownTypes(), elt),
                qualifiedTypeVariables);
    }

    @Override
    public QualifiedTypeMirror<Q> visitIntersection(IntersectionType type, Element elt) {
        return new QualifiedIntersectionType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.mapVisit(type.getBounds(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitNoType(NoType type, Element elt) {
        return new QualifiedNoType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()));
    }

    @Override
    public QualifiedTypeMirror<Q> visitNull(NullType type, Element elt) {
        return new QualifiedNullType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()));
    }

    @Override
    public QualifiedTypeMirror<Q> visitPrimitive(PrimitiveType type, Element elt) {
        return new QualifiedPrimitiveType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()));
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeVariable(TypeVariable type, Element elt) {
        return new QualifiedTypeVariable<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.visit(type.getUpperBound(), elt),
                this.visit(type.getLowerBound(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnion(UnionType type, Element elt) {
        return new QualifiedUnionType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.mapVisit(type.getAlternatives(), elt));
    }

    @Override
    public QualifiedTypeMirror<Q> visitWildcard(WildcardType type, Element elt) {
        return new QualifiedWildcardType<Q>(
                type,
                annotationConverter.fromAnnotations(type.getAnnotationMirrors()),
                this.visit(type.getExtendsBound(), elt),
                this.visit(type.getSuperBound(), elt));
    }
}
