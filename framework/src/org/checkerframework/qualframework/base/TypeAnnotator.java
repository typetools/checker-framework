package org.checkerframework.qualframework.base;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.qualframework.util.ExtendedArrayType;
import org.checkerframework.qualframework.util.ExtendedDeclaredType;
import org.checkerframework.qualframework.util.ExtendedErrorType;
import org.checkerframework.qualframework.util.ExtendedExecutableType;
import org.checkerframework.qualframework.util.ExtendedIntersectionType;
import org.checkerframework.qualframework.util.ExtendedNoType;
import org.checkerframework.qualframework.util.ExtendedNullType;
import org.checkerframework.qualframework.util.ExtendedPrimitiveType;
import org.checkerframework.qualframework.util.ExtendedTypeVariable;
import org.checkerframework.qualframework.util.ExtendedUnionType;
import org.checkerframework.qualframework.util.ExtendedWildcardType;
import org.checkerframework.qualframework.util.ExtendedParameterDeclaration;
import org.checkerframework.qualframework.util.ExtendedTypeDeclaration;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.ExtendedTypeVisitor;

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
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedParameterDeclaration;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeDeclaration;
import org.checkerframework.qualframework.util.QualifierContext;

/**
 * {@link DefaultQualifiedTypeFactory} component for annotating a {@link
 * ExtendedTypeMirror} with qualifiers.  The default implementation uses an
 * {@link AnnotationConverter} to process any annotations that are present on
 * the type, and uses the top qualifier if there are no annotations.
 */
public class TypeAnnotator<Q> implements ExtendedTypeVisitor<QualifiedTypeMirror<Q>, Void> {
    private final AnnotationConverter<Q> annotationConverter;
    private final Q defaultQual;

    private TypeAnnotatorAdapter<Q> adapter;
    protected QualifierContext<Q> qualContext;

    public TypeAnnotator(
            QualifierContext<Q> qualContext,
            AnnotationConverter<Q> annotationConverter,
            Q defaultQual) {

        this.annotationConverter = annotationConverter;
        this.defaultQual = defaultQual;
        this.qualContext = qualContext;
    }

    void setAdapter(TypeAnnotatorAdapter<Q> adapter) {
        this.adapter = adapter;
    }


    public AnnotationConverter<Q> getAnnotationConverter() {
        return annotationConverter;
    }


    public QualifiedTypeMirror<Q> visit(ExtendedTypeMirror type, Void p) {
        if (type == null) {
            return null;
        }
        return type.accept(this, null);
    }

    private List<QualifiedTypeMirror<Q>> mapVisit(
            List<? extends ExtendedTypeMirror> types, Void p) {
        List<QualifiedTypeMirror<Q>> result = new ArrayList<>();
        for (ExtendedTypeMirror type : types) {
            result.add(this.visit(type, null));
        }
        return result;
    }

    /**
     * Default handler to obtain an appropriate qualifier from an {@link
     * ExtendedTypeMirror}.  The default implementation uses the {@link
     * AnnotationConverter} to produce qualifier.
     */
    protected Q getQualifier(ExtendedTypeMirror type) {
        Q qual;

        // Sometimes the Framework makes us re-process partially-annotated
        // ATMs.  In that case, we should use the existing qualifier from the
        // last pass whenever one exists.
        qual = adapter.getExistingQualifier(type);

        // There was no previous annotation, so try to get one from the
        // AnnotationConverter.
        if (qual == null) {
            qual = annotationConverter.fromAnnotations(type.getAnnotationMirrors());
        }

        // As a last resort, default to top.
        // TODO: make the default an argument to this function (and maybe also
        // to the visitX methods), to make it easier to change the default in
        // specific situations (such as giving wildcard lower bounds a
        // different default from their upper bounds).
        if (qual == null) {
            qual = defaultQual;
        }

        return qual;
    }

    @Override
    public QualifiedTypeMirror<Q> visitArray(ExtendedArrayType type, Void p) {
        return new QualifiedArrayType<Q>(
                type,
                getQualifier(type),
                this.visit(type.getComponentType(), null));
    }

    @Override
    public QualifiedTypeMirror<Q> visitDeclared(ExtendedDeclaredType type, Void p) {
        QualifiedTypeMirror<Q> result = new QualifiedDeclaredType<Q>(
                type,
                getQualifier(type),
                this.mapVisit(type.getTypeArguments(), null));

        return result;
    }

    @Override
    public QualifiedTypeMirror<Q> visitError(ExtendedErrorType type, Void p) {
        throw new UnsupportedOperationException("saw unexpected ExtendedErrorType");
    }

    @Override
    public QualifiedTypeMirror<Q> visitExecutable(ExtendedExecutableType type, Void p) {
        // QualifiedExecutableType requires a list of QualifiedTypeVariables
        // rather than a list of generic QualifiedTypeMirrors.
        List<QualifiedParameterDeclaration<Q>> qualifiedParameters = new ArrayList<>();
        for (ExtendedParameterDeclaration typeVar : type.getTypeParameters()) {
            QualifiedParameterDeclaration<Q> qualifiedParam =
                (QualifiedParameterDeclaration<Q>)this.visit(typeVar, null);
            qualifiedParameters.add(qualifiedParam);
        }

        return new QualifiedExecutableType<Q>(
                type,
                this.mapVisit(type.getParameterTypes(), null),
                this.visit(type.getReceiverType(), null),
                this.visit(type.getReturnType(), null),
                this.mapVisit(type.getThrownTypes(), null),
                qualifiedParameters);
    }

    @Override
    public QualifiedTypeMirror<Q> visitIntersection(ExtendedIntersectionType type, Void p) {
        return new QualifiedIntersectionType<Q>(
                type,
                getQualifier(type),
                this.mapVisit(type.getBounds(), null));
    }

    @Override
    public QualifiedTypeMirror<Q> visitNoType(ExtendedNoType type, Void p) {
        return new QualifiedNoType<Q>(
                type,
                getQualifier(type));
    }

    @Override
    public QualifiedTypeMirror<Q> visitNull(ExtendedNullType type, Void p) {
        return new QualifiedNullType<Q>(
                type,
                getQualifier(type));
    }

    @Override
    public QualifiedTypeMirror<Q> visitPrimitive(ExtendedPrimitiveType type, Void p) {
        return new QualifiedPrimitiveType<Q>(
                type,
                getQualifier(type));
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeVariable(ExtendedTypeVariable type, Void p) {
        return new QualifiedTypeVariable<Q>(
                type,
                getQualifier(type));
    }

    @Override
    public QualifiedTypeMirror<Q> visitUnion(ExtendedUnionType type, Void p) {
        return new QualifiedUnionType<Q>(
                type,
                getQualifier(type),
                this.mapVisit(type.getAlternatives(), null));
    }

    @Override
    public QualifiedTypeMirror<Q> visitWildcard(ExtendedWildcardType type, Void p) {
        return new QualifiedWildcardType<Q>(
                type,
                this.visit(type.getExtendsBound(), null),
                this.visit(type.getSuperBound(), null));
    }

    @Override
    public QualifiedTypeMirror<Q> visitParameterDeclaration(ExtendedParameterDeclaration type, Void p) {
        return new QualifiedParameterDeclaration<Q>(
                type);
    }

    @Override
    public QualifiedTypeMirror<Q> visitTypeDeclaration(ExtendedTypeDeclaration type, Void p) {
        @SuppressWarnings("unchecked")
        List<? extends QualifiedParameterDeclaration<Q>> params =
            (List<? extends QualifiedParameterDeclaration<Q>>)(List<?>)
            this.mapVisit(type.getTypeParameters(), null);

        QualifiedTypeMirror<Q> result = new QualifiedTypeDeclaration<Q>(
                type,
                getQualifier(type),
                params);

        return result;
    }
}
