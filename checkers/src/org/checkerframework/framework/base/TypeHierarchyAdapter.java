package org.checkerframework.framework.base;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;

import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;

class TypeHierarchyAdapter<Q> extends checkers.types.TypeHierarchy {
    private TypeHierarchy<Q> underlying;
    private DefaultTypeHierarchy<Q> defaultUnderlying;
    private TypeMirrorConverter<Q> converter;

    public TypeHierarchyAdapter(TypeHierarchy<Q> underlying,
            TypeMirrorConverter<Q> converter,
            CheckerAdapter<Q> checker,
            QualifierHierarchyAdapter<Q>.Implementation qualifierHierarchy) {
        super(checker, qualifierHierarchy);
        this.underlying = underlying;

        if (underlying instanceof DefaultTypeHierarchy) {
            @SuppressWarnings("unchecked")
            DefaultTypeHierarchy<Q> defaultUnderlying =
                (DefaultTypeHierarchy<Q>)underlying;
            this.defaultUnderlying = defaultUnderlying;
        } else {
            this.defaultUnderlying = null;
        }

        this.converter = converter;
    }


    @Override
    public boolean isSubtype(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        return underlying.isSubtype(
                converter.getQualifiedType(a),
                converter.getQualifiedType(b));
    }

    boolean superIsSubtype(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtype(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    @Override
    protected boolean isSubtypeAsArrayComponent(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        return defaultUnderlying.isSubtypeAsArrayComponent(
                converter.getQualifiedType(a),
                converter.getQualifiedType(b));
    }

    boolean superIsSubtypeAsArrayComponent(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtypeAsArrayComponent(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    @Override
    protected boolean isSubtypeAsTypeArgument(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
        return defaultUnderlying.isSubtypeAsTypeArgument(
                converter.getQualifiedType(a),
                converter.getQualifiedType(b));
    }

    boolean superIsSubtypeAsTypeArgument(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtypeAsTypeArgument(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    boolean superIsSubtypeImpl(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return super.isSubtypeImpl(
                converter.getAnnotatedType(a),
                converter.getAnnotatedType(b));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType a, AnnotatedDeclaredType b) {
        return defaultUnderlying.isSubtypeTypeArguments(
                (QualifiedDeclaredType<Q>)converter.getQualifiedType(a),
                (QualifiedDeclaredType<Q>)converter.getQualifiedType(b));
    }

    @SuppressWarnings("unchecked")
    boolean superIsSubtypeTypeArguments(QualifiedDeclaredType<Q> a, QualifiedDeclaredType<Q> b) {
        return super.isSubtypeTypeArguments(
                (AnnotatedDeclaredType)converter.getAnnotatedType(a),
                (AnnotatedDeclaredType)converter.getAnnotatedType(b));
    }
}
